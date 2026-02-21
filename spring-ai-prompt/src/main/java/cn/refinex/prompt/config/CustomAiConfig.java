package cn.refinex.prompt.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义提示词模版的定界符
 *
 * @author refinex
 */
@Configuration
public class CustomAiConfig {

    /**
     * 全局替换定界符
     */
    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                // 覆盖默认的模板渲染器
                .defaultTemplateRenderer(StTemplateRenderer.builder()
                        // 自定义开始定界符
                        .startDelimiterToken('<')
                        // 自定义结束定界符
                        .endDelimiterToken('>')
                        .build())
                .build();
    }
}
