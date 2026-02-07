package cn.refinex.openai.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义 API Key 管理
 *
 * @author refinex
 */
@Configuration
public class CustomAiConfig {

    @Bean
    public OpenAiChatModel customChatModel() {
        // 1. 自定义 API Key 获取逻辑
        String myApiKey = getApiKeyFromDatabase();

        // 2. 构建底层 API 客户端
        OpenAiApi openAiApi = OpenAiApi.builder()
                // 等价于 https://api.openai.com
                //.baseUrl(OpenAiConnectionProperties.DEFAULT_BASE_URL)
                // 由于我这里是中转地址，所以需要制定下
                .baseUrl("https://code.ppchat.vip")
                // 动态注入 Key
                .apiKey(myApiKey)
                .build();

        // 3. 构建 ChatModel
        return OpenAiChatModel.builder()
                // 注入 OpenAiApi
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        // 设置模型（我的 Key 支持 .model("gpt-5.2")）
                        //.model(OpenAiApi.ChatModel.GPT_5)
                        // 设置模型
                        .model("gpt-5.2")
                        // 设置温度
                        .temperature(0.5)
                        .build())
                .build();
    }

    /**
     * 模拟从数据库获取 Key
     *
     * @return API Key
     */
    private String getApiKeyFromDatabase() {
        // 实际是从数据库读取，这里我们以从环境变量读为例
        // return "sk-custom-key-from-db...";

        return System.getenv("OPENAI_API_KEY");
    }
}
