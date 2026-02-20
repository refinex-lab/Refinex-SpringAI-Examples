package cn.refinex.anthropic.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Anthropic ChatClient 配置
 *
 * @author refinex
 */
@Configuration
public class ChatClientConfig {

    /**
     * 基于 AnthropicChatModel 构建专属 ChatClient Bean
     * <p>
     * 多模型共存时通过 {@code @Resource(name = "anthropicChatClient")} 注入
     *
     * @param anthropicChatModel Anthropic ChatModel
     * @return ChatClient
     */
    @Bean
    public ChatClient anthropicChatClient(AnthropicChatModel anthropicChatModel) {
        return ChatClient.builder(anthropicChatModel)
                .defaultSystem("你是 Claude，Anthropic 开发的 AI 助手。") // 可选：全局默认 System Prompt
                .build();
    }
}
