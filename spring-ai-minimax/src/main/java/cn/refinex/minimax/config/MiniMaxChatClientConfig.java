package cn.refinex.minimax.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MiniMax ChatClient 配置
 *
 * @author refinex
 */
@Slf4j
@Configuration
public class MiniMaxChatClientConfig {

    /**
     * 基于 MiniMaxChatModel 构建专属 ChatClient Bean
     * 多模型共存时通过 @Resource(name = "miniMaxChatClient") 注入
     */
    @Bean
    public ChatClient miniMaxChatClient(MiniMaxChatModel miniMaxChatModel) {
        return ChatClient.builder(miniMaxChatModel)
                .defaultSystem("你是 MiniMax 提供的 AI 助手，请用中文回答问题。")
                .build();
    }
}
