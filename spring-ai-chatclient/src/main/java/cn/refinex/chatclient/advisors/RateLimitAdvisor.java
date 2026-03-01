package cn.refinex.chatclient.advisors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流 Advisor
 * <p>
 * WHY 用 CallAdvisor 而不是 BaseAdvisor：
 * 限流需要「阻断」能力——当请求超频时，不调用 chain.nextCall()，
 * 直接返回限流提示。BaseAdvisor 的 before/after 模型无法实现短路。
 *
 * @author refinex
 */
public class RateLimitAdvisor implements CallAdvisor, StreamAdvisor {

    // WHY ConcurrentHashMap + AtomicInteger：
    // 多线程并发安全，且 AtomicInteger 的 CAS 操作比 synchronized 更高效
    private final Map<String, AtomicInteger> callCounts = new ConcurrentHashMap<>();

    // 每分钟最大调用次数
    // WHY 60s：符合一般应用的限流要求，可根据需要调整
    private final int maxCallsPerMinute;

    // 执行顺序
    // WHY 500：默认顺序，可根据需要调整
    private final int order;

    /**
     * 限流 Advisor 构造函数
     * <p>
     * WHY：默认顺序为 500，可根据需要调整。
     *
     * @param maxCallsPerMinute 每分钟最大调用次数
     */
    public RateLimitAdvisor(int maxCallsPerMinute) {
        this(maxCallsPerMinute, 500);
    }

    /**
     * 限流 Advisor 构造函数
     *
     * @param maxCallsPerMinute 每分钟最大调用次数
     * @param order             执行顺序
     */
    public RateLimitAdvisor(int maxCallsPerMinute, int order) {
        this.maxCallsPerMinute = maxCallsPerMinute;
        this.order = order;
    }

    /**
     * 限流 Advisor 调用方法
     * <p>
     * WHY：检查当前请求是否超过限流阈值。
     *
     * @param chatClientRequest 聊天客户端请求
     * @param callAdvisorChain  调用 Advisor 链
     * @return 聊天客户端响应
     */
    @NonNull
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, @NonNull CallAdvisorChain callAdvisorChain) {
        // 获取对话 ID，默认值为 "global"
        // WHY 全局限流：当未指定 conversationId 时，对所有对话进行限流
        String conversationId = (String) chatClientRequest.context().getOrDefault(ChatMemory.CONVERSATION_ID, "global");

        if (isRateLimited(conversationId)) {
            // ⚡ 短路：不调用 chatClientRequest.nextCall()，直接返回限流响应
            // WHY 构造完整的 ChatClientResponse 而不是抛异常：
            // 抛异常会破坏 Advisor 链的正常执行，上游 Advisor 的 after() 不会被调用
            return buildRateLimitResponse(chatClientRequest);
        }

        // 未超频，继续执行链路
        return callAdvisorChain.nextCall(chatClientRequest);
    }

    /**
     * 限流 Advisor 流式调用方法
     * <p>
     * WHY：检查当前请求是否超过限流阈值。
     *
     * @param chatClientRequest  聊天客户端请求
     * @param streamAdvisorChain 流式 Advisor 链
     * @return 聊天客户端响应流
     */
    @NonNull
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, @NonNull StreamAdvisorChain streamAdvisorChain) {
        // 获取对话 ID，默认值为 "global"
        // WHY 全局限流：当未指定 conversationId 时，对所有对话进行限流
        String conversationId = (String) chatClientRequest.context().getOrDefault(ChatMemory.CONVERSATION_ID, "global");

        if (isRateLimited(conversationId)) {
            // ⚡ 短路：不调用 chatClientRequest.nextStream()，直接返回限流响应
            return Flux.just(buildRateLimitResponse(chatClientRequest));
        }

        // 未超频，继续执行链路
        return streamAdvisorChain.nextStream(chatClientRequest);
    }

    /**
     * 检查是否超过限流阈值
     *
     * @param conversationId 对话 ID
     * @return 是否超过限流阈值
     */
    private boolean isRateLimited(String conversationId) {
        // 每个对话 ID 独立计数, 避免不同对话之间的调用次数累加
        AtomicInteger count = callCounts.computeIfAbsent(conversationId, k -> new AtomicInteger(0));
        // 每个请求增加调用次数后, 检查是否超过限流阈值
        return count.incrementAndGet() > maxCallsPerMinute;
        // 生产环境应使用 Redis + 滑动窗口算法替代内存计数
    }

    /**
     * 构建限流响应
     *
     * @param request 请求对象
     * @return 限流响应
     */
    private ChatClientResponse buildRateLimitResponse(ChatClientRequest request) {
        String message = String.format("⚠️ 请求过于频繁，每分钟最多 %d 次调用。请稍后再试。", maxCallsPerMinute);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage(message))));
        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .context(request.context())
                .build();
    }

    /**
     * 获取 Advisor 名称
     * <p>
     * WHY：用于日志记录和调试。
     *
     * @return Advisor 名称
     */
    @NonNull
    @Override
    public String getName() {
        return "RateLimitAdvisor";
    }

    /**
     * 获取 Advisor 执行顺序
     * <p>
     * WHY：确保在 MemoryAdvisor 之前执行，防止超频请求写入 Memory。
     *
     * @return 执行顺序
     */
    @Override
    public int getOrder() {
        return order;
    }
}
