package cn.refinex.anthropic.controller;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Anthropic Claude 聊天 Controller
 *
 * @author refinex
 */
@Slf4j
@RestController
@RequestMapping("/anthropic")
@RequiredArgsConstructor
public class AnthropicController {

    /**
     * 通过 @Resource 按名称注入，避免与其他 ChatClient Bean 冲突
     */
    @Resource(name = "anthropicChatClient")
    private ChatClient chatClient;

    /**
     * 直接注入 AnthropicChatModel（底层调用，适合需要访问原始 API 参数的场景）
     */
    private final AnthropicChatModel chatModel;

    /**
     * 同步调用（使用 ChatModel）
     *
     * @param message 聊天内容
     * @return 聊天回复
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "你是什么模型？") String message) {
        return chatModel.call(message);
    }

    /**
     * 同步调用（使用 ChatClient，推荐方式）
     *
     * @param message 聊天内容
     * @return 聊天回复
     */
    @GetMapping("/chat2")
    public String chat2(@RequestParam(value = "message", defaultValue = "你是什么模型？") String message) {
        return chatClient.prompt(message).call().content();
    }

    /**
     * 流式调用（SSE）
     * <p>
     * Claude 支持流式输出，适合长文本生成场景
     *
     * @param message 聊天内容
     * @return 流式聊天回复
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam(defaultValue = "介绍一下你自己") String message) {
        return chatClient.prompt(message).stream().content();
    }
}
