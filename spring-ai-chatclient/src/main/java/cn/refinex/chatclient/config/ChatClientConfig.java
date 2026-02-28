package cn.refinex.chatclient.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 配置类
 * <p>
 * WHY：将 ChatClient 的构建集中在配置类中，而不是分散在各个 Controller 的构造器中。
 * 好处：所有 ChatClient 的默认行为在一处定义和审计。
 *
 * @author refinex
 */
@Configuration
public class ChatClientConfig {

    /**
     * ChatMemoryRepository — 对话记忆的存储后端
     * <p>
     * WHY：InMemory 仅适用于开发环境。
     * 生产环境应替换为 JdbcChatMemoryRepository 或 CassandraChatMemoryRepository。
     */
    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    /**
     * ChatMemory — 对话记忆的窗口策略
     * <p>
     * WHY：maxMessages=20 意味着保留最近 20 条消息。
     * 这是 Token 成本和上下文质量的平衡点。
     * 太小 → AI 忘记重要上下文；太大 → Token 消耗失控。
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                // 设置对话记忆的存储后端
                .chatMemoryRepository(repository)
                // 设置对话记忆的窗口大小
                // 20 条消息是一个合理的默认值，可根据实际场景调整
                .maxMessages(20)
                .build();
    }

    /**
     * 通用对话 ChatClient
     * <p>
     * Advisor 链配置（按执行顺序）：
     * 1. MessageChatMemoryAdvisor — 加载历史消息 + 保存本轮对话
     * 2. SimpleLoggerAdvisor — 记录请求/响应日志
     */
    @Bean
    public ChatClient generalChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                // 默认 System Prompt：定义 AI 角色和行为约束
                .defaultSystem("""
                        你是 Refinex 平台的 AI 技术助手。
                        回答规范：
                        1. 使用中文回答，技术术语保留英文原文
                        2. 给出具体的代码示例，不要空泛描述
                        3. 如果不确定，明确说明而不是猜测
                        """)
                // 默认 Advisors：所有请求自动携带 Memory + Logger
                .defaultAdvisors(
                        // Memory Advisor —— 自动管理对话上下文
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // Logger Advisor —— 记录请求/响应日志（建议放在链尾）
                        new SimpleLoggerAdvisor()
                )
                // 默认 Options：全局温度和 Token 限制
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.7)
                        .maxTokens(2000)
                        .build())
                .build();
    }
}
