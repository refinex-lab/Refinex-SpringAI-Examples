package cn.refinex.openai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring AI Open AI 集成 Controller
 *
 * @author refinex
 */
@RestController
@RequestMapping("/ai")
public class ChatController {

    // 推荐使用 ChatClient，它是对 ChatModel 的高级封装
    private final ChatClient chatClient;

    // 自己注入的 OpenAiChatModel
    private final OpenAiChatModel customChatModel;

    // 构造器注入自动配置好的 Builder
    public ChatController(ChatClient.Builder builder, OpenAiChatModel customChatModel) {
        this.chatClient = builder.build();
        this.customChatModel = customChatModel;
    }

    @GetMapping("/simple")
    public String simpleChat(@RequestParam String msg) {
        return chatClient.prompt(msg).call().content();
    }

    @GetMapping("/custom")
    public String customChat(@RequestParam String msg) {
        return customChatModel.call(msg);
    }

    @GetMapping("/creative-chat")
    public String creativeChat(@RequestParam String msg) {
        return chatClient.prompt(msg)
                // 运行时覆盖
                .options(OpenAiChatOptions.builder()
                        // 将温度调高到 1.0，让 AI 更疯狂
                        .temperature(1.0)
                        // 甚至可以临时换模型（我的 Key 支持 .model("gpt-5.2")）
                        // .model(OpenAiApi.DEFAULT_CHAT_MODEL)
                        .model("gpt-5.2")
                        .build())
                .call()
                .content();
    }
}
