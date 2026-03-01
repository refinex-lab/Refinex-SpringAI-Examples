package cn.refinex.chatclient.advisors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存 Advisor
 * <p>
 * WHY 用 CallAdvisor 而不是 BaseAdvisor：
 * 缓存命中时需要跳过 AI 调用（不调用 chain.nextCall()），
 * 这是 BaseAdvisor 的 before/after 模型无法做到的。
 * <p>
 * WHY 只缓存 call() 不缓存 stream()：
 * 流式响应的缓存需要先完整聚合再缓存，实现复杂且延迟收益不明确。
 * 生产环境建议只对同步调用做缓存。
 *
 * @author refinex
 */
public class PromptCacheAdvisor implements CallAdvisor, StreamAdvisor {

    // 缓存存储：key 为缓存 key，value 为缓存条目
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // 缓存过期时间
    private final Duration ttl;

    // 执行顺序
    private final int order;

    /**
     * 缓存 Advisor 构造函数
     *
     * @param ttl 缓存过期时间
     */
    public PromptCacheAdvisor(Duration ttl) {
        this(ttl, 450);
    }

    /**
     * 缓存 Advisor 构造函数
     *
     * @param ttl   缓存过期时间
     * @param order 执行顺序
     */
    public PromptCacheAdvisor(Duration ttl, int order) {
        this.ttl = ttl;
        this.order = order;
    }

    /**
     * 缓存 Advisor 调用方法
     *
     * @param chatClientRequest 聊天客户端请求
     * @param callAdvisorChain  调用 Advisor 链
     * @return 聊天客户端响应
     */
    @NonNull
    @Override
    public ChatClientResponse adviseCall(@NonNull ChatClientRequest chatClientRequest, @NonNull CallAdvisorChain callAdvisorChain) {
        String cacheKey = buildCacheKey(chatClientRequest);

        // 检查缓存
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            // ⚡ 缓存命中：短路，不调用下游
            return entry.response();
        }

        // 缓存未命中，继续执行链路
        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);

        // 缓存响应
        cache.put(cacheKey, new CacheEntry(response, System.currentTimeMillis() + ttl.toMillis()));

        return response;
    }

    /**
     * 缓存 Advisor 流式调用方法
     *
     * @param chatClientRequest  聊天客户端请求
     * @param streamAdvisorChain 流式 Advisor 链
     * @return 聊天客户端响应流
     */
    @NonNull
    @Override
    public Flux<ChatClientResponse> adviseStream(@NonNull ChatClientRequest chatClientRequest, @NonNull StreamAdvisorChain streamAdvisorChain) {
        // 流式调用不走缓存，直接透传
        return streamAdvisorChain.nextStream(chatClientRequest);
    }

    /**
     * 构建缓存 key
     *
     * @param request 请求对象
     * @return 缓存 key
     */
    private String buildCacheKey(ChatClientRequest request) {
        // WHY 只用 userMessage 作为 key：
        // System Prompt 通常不变，Options 的差异应导致缓存不命中
        // 生产环境应加入 System Prompt hash 和关键 Options 作为 key 的一部分
        return request.prompt().getUserMessage().getText().trim().toLowerCase();
    }

    /**
     * 缓存名称
     */
    @NonNull
    @Override
    public String getName() {
        return "PromptCacheAdvisor";
    }

    /**
     * 缓存顺序
     */
    @Override
    public int getOrder() {
        return this.order;
    }

    /**
     * 缓存条目
     */
    record CacheEntry(
            // 缓存响应
            ChatClientResponse response,
            // 过期时间（毫秒）
            long expiresAt
    ) {

        /**
         * 是否过期
         */
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
