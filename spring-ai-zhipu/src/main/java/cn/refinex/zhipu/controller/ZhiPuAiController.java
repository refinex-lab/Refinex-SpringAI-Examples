package cn.refinex.zhipu.controller;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * ZhiPu AI 聊天 Controller
 *
 * @author refinex
 */
@Slf4j
@RestController
@RequestMapping("/zhipuai")
@RequiredArgsConstructor
public class ZhiPuAiController {

    /**
     * 按名称注入，避免与其他 ChatClient Bean 冲突
     */
    @Resource(name = "zhiPuAiChatClient")
    private ChatClient chatClient;

    /**
     * 直接注入 ZhiPuAiChatModel（底层调用，适合需要访问原始 API 参数的场景）
     */
    private final ZhiPuAiChatModel chatModel;

    /**
     * 同步调用（ChatModel 直接调用）
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(defaultValue = "你好，介绍一下你自己") String message) {
        return chatModel.call(message);
    }

    /**
     * 同步调用（ChatClient，推荐方式）
     */
    @GetMapping("/chat2")
    public String chat2(@RequestParam(defaultValue = "你好，介绍一下你自己") String message) {
        return chatClient.prompt(message).call().content();
    }

    /**
     * 流式调用（SSE 打字机效果）
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam(defaultValue = "讲个冷笑话") String message) {
        return chatClient.prompt(message).stream().content();
    }

    /**
     * 运行时切换模型（A/B 测试 / 按场景动态选模型）
     */
    @GetMapping("/chat-custom")
    public String chatWithCustomOptions(@RequestParam(defaultValue = "生成 5 个海盗名字") String message) {
        ChatResponse response = chatModel.call(
                new Prompt(
                        message,
                        ZhiPuAiChatOptions.builder()
                                .model("glm-4-air")    // 临时切换为高性价比版
                                .temperature(0.5)
                                .build()
                )
        );
        return response.getResult().getOutput().getText();
    }
}
