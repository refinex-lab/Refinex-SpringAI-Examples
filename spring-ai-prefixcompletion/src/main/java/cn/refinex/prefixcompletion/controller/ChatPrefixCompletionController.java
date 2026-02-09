package cn.refinex.prefixcompletion.controller;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Chat Prefix Completion Controller
 *
 * @author refinex
 */
@RestController
public class ChatPrefixCompletionController {

    private final DeepSeekChatModel chatModel;

    /**
     * 构造注入
     *
     * @param chatModel Chat Model
     */
    public ChatPrefixCompletionController(DeepSeekChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 对话前缀续写：强制输出 Java 代码
     */
    @GetMapping("/chatPrefixCompletion")
    public String chatPrefixCompletion(@RequestParam("message") String message) {
        UserMessage userMessage = new UserMessage(message);

        // assistant 前缀：以 Java 代码块开头
        DeepSeekAssistantMessage deepSeekAssistantMessage = DeepSeekAssistantMessage.prefixAssistantMessage("```java\n");
        // 把 `prefix=true` 带上
        deepSeekAssistantMessage.setPrefix(true);

        Prompt prompt = new Prompt(
                // 确保最后一条消息是 assistant 消息
                List.of(userMessage, deepSeekAssistantMessage),
                ChatOptions.builder()
                        .stopSequences(List.of("```")) // 设置停止序列为 ```，即遇到 ``` 时停止生成
                        .build()
        );

        ChatResponse response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }

    /**
     * 续写被截断的输出（将上一轮已生成内容作为前缀）
     */
    @GetMapping("/chatPrefixContinuation")
    public String chatPrefixContinuation(@RequestParam("partial") String partial) {
        UserMessage userMessage = new UserMessage("请把这段说明补全...");

        // assistant 前缀：将上一轮已生成内容作为前缀
        DeepSeekAssistantMessage deepSeekAssistantMessage = DeepSeekAssistantMessage.prefixAssistantMessage(partial);
        // 把 `prefix=true` 带上
        deepSeekAssistantMessage.setPrefix(true);

        // 确保最后一条消息是 assistant 消息
        List<Message> messages = List.of(userMessage, deepSeekAssistantMessage);

        Prompt prompt = new Prompt(messages);
        ChatResponse response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }
}
