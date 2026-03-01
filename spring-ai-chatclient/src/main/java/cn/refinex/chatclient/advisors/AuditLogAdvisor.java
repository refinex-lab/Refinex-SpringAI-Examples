package cn.refinex.chatclient.advisors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.lang.NonNull;

/**
 * 生产级日志审计 Advisor
 * <p>
 * WHY 用 BaseAdvisor 而不是 CallAdvisor：
 * 1. 日志审计只需要观察请求和响应，不需要阻断链路或循环执行。
 * 2. BaseAdvisor 的 before/after 模式最匹配这个需求，且自动处理流式场景。
 *
 * @author refinex
 */
public class AuditLogAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAdvisor.class);

    // WHY Order=900：确保记录的是经过所有 Advisor 处理后的最终 Prompt
    private static final int DEFAULT_ORDER = 900;

    // 常量定义：用于在请求上下文中传递开始时间
    private static final String AUDIT_START_TIME_KEY = "audit_start_time";

    // 允许自定义 Order，默认 900
    private final int order;

    /**
     * 使用默认 Order 构造
     */
    public AuditLogAdvisor() {
        this(DEFAULT_ORDER);
    }

    /**
     * 使用自定义 Order 构造
     *
     * @param order 执行顺序，默认 900
     */
    public AuditLogAdvisor(int order) {
        this.order = order;
    }

    /**
     * 记录请求日志
     *
     * @param chatClientRequest 请求对象
     * @param advisorChain      执行链
     * @return 请求对象
     */
    @NonNull
    @Override
    public ChatClientRequest before(@NonNull ChatClientRequest chatClientRequest, @NonNull AdvisorChain advisorChain) {
        // 记录请求时间戳到 context，供 after() 计算耗时
        long startTime = System.currentTimeMillis();

        String userMessageText = chatClientRequest.prompt().getUserMessage() != null
                ? truncate(chatClientRequest.prompt().getUserMessage().getText(), 200)
                : "null";
        String systemMessageText = chatClientRequest.prompt().getSystemMessage() != null
                ? truncate(chatClientRequest.prompt().getSystemMessage().getText(), 100)
                : "null";

        log.info("[AI-AUDIT] 请求开始 | userMessage={} | systemMessage={}", userMessageText, systemMessageText);

        // 通过 context 传递 startTime 给 after() 阶段
        return chatClientRequest.mutate()
                .context(AUDIT_START_TIME_KEY, startTime)
                // 也支持 Map 方式，一般用于传递多个参数
                //.context(Map.of(AUDIT_START_TIME_KEY, startTime))
                .build();
    }

    /**
     * 记录响应日志
     *
     * @param chatClientResponse 响应对象
     * @param advisorChain       执行链
     * @return 响应对象
     */
    @NonNull
    @Override
    public ChatClientResponse after(@NonNull ChatClientResponse chatClientResponse, @NonNull AdvisorChain advisorChain) {
        // 从 before() 阶段传递过来的 startTime，用于计算耗时
        long startTime = (long) chatClientResponse.context().getOrDefault(AUDIT_START_TIME_KEY, 0L);
        long duration = System.currentTimeMillis() - startTime;

        // 提取 Token 用量信息和响应文本
        ChatResponse chatResponse = chatClientResponse.chatResponse();
        String usageInfo = extractUsageInfo(chatResponse);
        String responseText = extractResponseText(chatResponse);

        log.info("[AI-AUDIT] 请求完成 | duration={}ms | tokens=[{}] | response={}",
                duration, usageInfo, responseText);

        return chatClientResponse;
    }

    /**
     * 提取 Token 用量信息
     *
     * @param chatResponse 响应对象
     * @return Token 用量字符串
     */
    private String extractUsageInfo(ChatResponse chatResponse) {
        if (chatResponse != null && chatResponse.getMetadata().getUsage() != null) {
            Usage usage = chatResponse.getMetadata().getUsage();
            return String.format("prompt=%d, completion=%d, total=%d",
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens());
        }
        return "N/A";
    }

    /**
     * 提取响应文本
     *
     * @param chatResponse 响应对象
     * @return 响应文本
     */
    private String extractResponseText(ChatResponse chatResponse) {
        if (chatResponse != null) {
            return truncate(chatResponse.getResult().getOutput().getText(), 200);
        }
        return "null";
    }

    /**
     * 返回 Advisor 执行顺序, 值越小越先执行 before，越晚执行 after
     */
    @Override
    public int getOrder() {
        return this.order;
    }

    /**
     * 截断字符串，超过 maxLen 时添加省略号
     *
     * @param text   原始字符串
     * @param maxLen 最大长度
     * @return 截断后的字符串
     */
    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return "null";
        }

        return text.length() > maxLen ? "%s...".formatted(text.substring(0, maxLen)) : text;
    }
}
