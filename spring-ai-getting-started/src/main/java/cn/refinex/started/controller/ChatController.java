package cn.refinex.started.controller;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Objects;

/**
 * 聊天控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    // 注入的是 ChatModel 接口，不是具体实现类
    // 具体实现由 spring.ai.model.chat 属性决定
    private final ChatModel chatModel;

    /**
     * 聊天控制器构造函数注入
     *
     * @param chatModel 聊天模型
     */
    public ChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 聊天接口（同步调用）
     *
     * @param message 用户输入的消息
     * @return 回复消息
     */
    @GetMapping("/ask")
    public Map<String, String> ask(@RequestParam(defaultValue = "你好") String message) {
        // 使用 ChatOptions（通用接口），不依赖任何 Provider 专有 API
        ChatResponse response = chatModel.call(
                // 构造 Prompt 对象，封装了用户输入和 ChatOptions
                new Prompt(
                        // 用户输入
                        message,
                        // ChatOptions 配置
                        ChatOptions.builder()
                                // 温度参数，控制回复的随机性，0.7 是一个常用值
                                // 温度参数，控制生成文本的随机性
                                // 范围：0.0 到 2.0
                                .temperature(0.7)
                                // 最大令牌数，限制生成文本的长度
                                .maxTokens(500)
                                .build()
                )
        );

        // 从响应中提取文本内容，确保不为 null
        return Map.of("reply", Objects.requireNonNull(response.getResult().getOutput().getText()));
    }

    /**
     * 聊天接口（流式调用）
     *
     * @param message 用户输入的消息
     * @return 回复消息流
     */
    @GetMapping("/stream")
    public Flux<String> stream(@RequestParam(defaultValue = "讲一个笑话") String message) {
        // 流式调用同样 Provider 无关
        return chatModel.stream(message);
    }
}
