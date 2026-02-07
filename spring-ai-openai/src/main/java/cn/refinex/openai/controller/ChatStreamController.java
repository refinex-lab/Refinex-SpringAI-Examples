package cn.refinex.openai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Spring AI Open AI Stream Controller
 *
 * @author refinex
 */
@RestController
@RequestMapping("/ai")
public class ChatStreamController {

    // 推荐使用 ChatClient，它是对 ChatModel 的高级封装
    private final ChatClient chatClient;

    // 自己构建的 OpenAiChatModel Bean
    private final OpenAiChatModel customChatModel;

    // 构造器注入自动配置好的 Builder
    public ChatStreamController(ChatClient.Builder builder, OpenAiChatModel customChatModel) {
        this.chatClient = builder.build();
        this.customChatModel = customChatModel;
    }

    // ❌ 典型的 “伪流式” 写法
    @GetMapping("/chatStream")
    public Flux<ChatResponse> chatStream(@RequestParam String message) {
        Prompt prompt = new Prompt(new UserMessage(message));

        // 直接返回 ChatResponse 对象流
        return chatClient.prompt(prompt).stream().chatResponse();
    }

    /**
     * ✅ 标准的流式聊天接口
     * 关键点 1: produces = MediaType.TEXT_EVENT_STREAM_VALUE
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String message) {
        Prompt prompt = new Prompt(new UserMessage(message));

        // 关键点 2: 将复杂的 ChatResponse 转换为简单的 String
        return customChatModel.stream(prompt)
                .map(response -> {
                    // 提取每个 chunk 中的文本内容
                    String text = response.getResult().getOutput().getText();
                    return text != null ? text : "";
                })
                // 关键点 3: 过滤掉可能产生的空帧（OpenAI 偶尔会发空包）
                .filter(text -> !text.isEmpty());
    }

    @GetMapping(value = "/stream-client", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamWithClient(@RequestParam String msg) {
        return chatClient.prompt(msg)
                // 开启流式模式
                .stream()
                // 自动帮你把 ChatResponse 拆解成 String
                .content();
    }
}
