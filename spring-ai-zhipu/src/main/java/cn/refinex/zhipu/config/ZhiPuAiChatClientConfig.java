package cn.refinex.zhipu.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ZhiPu AI ChatClient 配置
 *
 * @author refinex
 */
@Configuration
public class ZhiPuAiChatClientConfig {

    /**
     * 构建 ZhiPu AI 专属 ChatClient Bean
     * 多模型共存时通过 @Resource(name = "zhiPuAiChatClient") 注入
     */
    @Bean
    public ChatClient zhiPuAiChatClient(ZhiPuAiChatModel zhiPuAiChatModel) {
        return ChatClient.builder(zhiPuAiChatModel)
                .defaultSystem("你是智谱 AI 提供的 AI 助手，请用中文回答。")
                .build();
    }
}
