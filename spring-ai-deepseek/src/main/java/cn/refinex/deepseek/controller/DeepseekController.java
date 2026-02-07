package cn.refinex.deepseek.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Spring AI DeepSeek Controller
 *
 * @author refinex
 */
@Slf4j
@RestController
@RequestMapping("/deepseek")
@RequiredArgsConstructor
public class DeepseekController {

    /**
     * Spring Boot 自动装配好的 DeepSeek 模型
     */
    private final DeepSeekChatModel chatModel;

    /**
     * 简单聊天接口
     */
    @GetMapping("/chat")
    public String chat(@RequestParam("message") String message) {
        log.info("DeepSeek 请求内容: {}", message);

        // 发起调用，Spring AI 会自动处理 JSON 序列化和反序列化
        String response = chatModel.call(message);

        log.info("DeepSeek 响应内容: {}", response);
        return response;
    }

    /**
     * 进阶：流式聊天 (强烈推荐)
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String message) {
        return chatModel.stream(message);
    }
}
