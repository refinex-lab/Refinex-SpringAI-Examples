package cn.refinex.quickstart.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 聊天 控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;

    // 构造器注入 Builder 并构建 Client
    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String msg) {
        return chatClient.prompt(msg).call().content();
    }
}
