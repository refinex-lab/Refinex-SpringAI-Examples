package cn.refinex.multimodel.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

/**
 * ChatClient Config
 *
 * @author refinex
 */
@Configuration
public class ChatClientConfig {

    /**
     * 手动提供 ChatClient.Builder，避免自动配置依赖唯一 ChatModel
     * 建议使用最常用的模型作为 “默认底座”。
     */
    @Bean
    @Scope("prototype") // 将 ChatClient.Builder 定义为 prototype 级别，确保每次注入的都是新的实例
    @ConditionalOnMissingBean // 确保在没有 ChatClient.Builder Bean 的情况下才创建
    ChatClient.Builder chatClientBuilder(OpenAiChatModel chatModel) { // 默认使用 OpenAiChatModel 作为基座
        return ChatClient.builder(chatModel);
    }

    /**
     * OpenAI 专用 Client
     */
    @Primary // 作为默认的 ChatClient Bean
    @Bean
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是一个由 OpenAI 驱动的逻辑专家。")
                .build();
    }

    /**
     * DeepSeek 专用 Client
     */
    @Bean
    public ChatClient deepSeekChatClient(DeepSeekChatModel deepSeekChatModel) {
        return ChatClient.builder(deepSeekChatModel)
                .defaultSystem("你是一个由 DeepSeek 驱动的中文助手。")
                .build();
    }
}
