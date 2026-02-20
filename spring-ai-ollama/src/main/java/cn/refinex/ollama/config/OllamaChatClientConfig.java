package cn.refinex.ollama.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ollama ChatClient 配置
 *
 * @author refinex
 */
@Configuration
public class OllamaChatClientConfig {

    /**
     * 基于 OllamaChatModel 构建专属 ChatClient Bean
     * 多模型共存时通过 @Resource(name = "ollamaChatClient") 注入
     */
    @Bean
    public ChatClient ollamaChatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("你是一个本地运行的 AI 助手，请用中文简洁准确地回答问题。")
                .build();
    }
}
