package cn.refinex.multimodel.controller;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 混合聊天控制器
 *
 * @author refinex
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class HybridChatController {

    /**
     * 明确使用 DeepSeek 聊天客户端，也可以使用注解组合 @Autowired + @Qualifier 指定
     */
    @Resource(name = "deepSeekChatClient")
    private ChatClient deepSeekClient;

    /**
     * 我们在 {@code ChatClientConfig} 中定义了 openAiChatClient 作为 @Primary 这里直接注入即可
     */
    private final ChatClient openAiClient;

    /**
     * 也可以直接使用底层的 DeepSeekChatModel
     */
    private final DeepSeekChatModel deepSeekModel;

    @GetMapping("/chat/deepseek")
    public String deepSeekChat(@RequestParam String message) {
        log.info("调用 DeepSeek...");
        return deepSeekClient.prompt(message).call().content();
    }

    @GetMapping("/chat/openai")
    public String openAiChat(@RequestParam String message) {
        log.info("调用 OpenAI...");
        return openAiClient.prompt(message).call().content();
    }

    @GetMapping("/chat/raw")
    public String rawChat(@RequestParam String message) {
        return deepSeekModel.call(message);
    }

}
