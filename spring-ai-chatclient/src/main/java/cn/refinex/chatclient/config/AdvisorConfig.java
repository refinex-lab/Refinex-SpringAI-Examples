package cn.refinex.chatclient.config;

import cn.refinex.chatclient.advisors.AuditLogAdvisor;
import cn.refinex.chatclient.advisors.PromptCacheAdvisor;
import cn.refinex.chatclient.advisors.RateLimitAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Advisor 配置类
 *
 * @author refinex
 */
@Configuration
public class AdvisorConfig {

    /**
     * 配置 ChatClient 的默认 Advisor 链
     * <p>
     * 1. 对话历史管理：MessageChatMemoryAdvisor
     * 2. 缓存优化：PromptCacheAdvisor
     * 3. 限流控制：RateLimitAdvisor
     * 4. 审计日志：AuditLogAdvisor
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(
                        // Order=100 — 最先加载对话历史
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // Order=450 — 缓存命中时跳过后续所有 Advisor 和 AI 调用
                        new PromptCacheAdvisor(Duration.ofMillis(5)),
                        // Order=500 — 限流检查
                        new RateLimitAdvisor(60),
                        // Order=900 — 最后记录完整请求/响应日志
                        new AuditLogAdvisor()
                ).build();
    }
}
