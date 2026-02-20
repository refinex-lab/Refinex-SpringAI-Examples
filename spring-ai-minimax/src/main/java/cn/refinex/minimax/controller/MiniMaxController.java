package cn.refinex.minimax.controller;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.ai.minimax.MiniMaxChatOptions;
import org.springframework.ai.minimax.api.MiniMaxApi;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * MiniMax 聊天 Controller
 *
 * @author refinex
 */
@Slf4j
@RestController
@RequestMapping("/minimax")
@RequiredArgsConstructor
public class MiniMaxController {

    /**
     * 按名称注入，避免与其他 ChatClient Bean 冲突
     */
    @Resource(name = "miniMaxChatClient")
    private ChatClient chatClient;

    /**
     * 直接注入 MiniMaxChatModel（底层调用，适合需要访问原始 API 参数的场景）
     */
    private final MiniMaxChatModel chatModel;

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
     * 流式调用（SSE）
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam(defaultValue = "讲个冷笑话") String message) {
        return chatClient.prompt(message).stream().content();
    }

    /**
     * 运行时覆盖模型参数（适合 A/B 测试或临时切换模型）
     */
    @GetMapping("/chat-custom")
    public String chatWithCustomOptions(@RequestParam(defaultValue = "生成 5 个海盗名字") String message) {
        ChatResponse response = chatModel.call(
                new Prompt(
                        message,
                        MiniMaxChatOptions.builder()
                                .model(MiniMaxApi.ChatModel.ABAB_6_5_S_Chat.getValue()) // 临时切换为速度版
                                .temperature(0.5)
                                .build()
                )
        );
        return response.getResult().getOutput().getText();
    }

    /**
     * 联网搜索示例
     * MiniMax 内置 WebSearch 工具，通过 FunctionTool 传入
     */
    @GetMapping("/web-search")
    public String webSearch(@RequestParam(defaultValue = "2024年奥运会美国获得多少金牌？") String message) {
        // WebSearch 工具
        List<MiniMaxApi.FunctionTool> tools = List.of(MiniMaxApi.FunctionTool.webSearchFunctionTool());

        MiniMaxChatOptions options = MiniMaxChatOptions.builder()
                .model(MiniMaxApi.ChatModel.ABAB_6_5_S_Chat.getValue())
                // 使用 WebSearch 工具
                .tools(tools)
                .build();

        return chatModel.call(new Prompt(new UserMessage(message), options))
                .getResult()
                .getOutput()
                .getText();
    }

    /**
     * 联网搜索示例 (SSE)
     * MiniMax 内置 WebSearch 工具，通过 FunctionTool 传入
     */
    @GetMapping(value = "/web-search-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> webSearchStream(@RequestParam(defaultValue = "2024年奥运会美国获得多少金牌？") String message) {
        // WebSearch 工具
        List<MiniMaxApi.FunctionTool> tools = List.of(MiniMaxApi.FunctionTool.webSearchFunctionTool());

        MiniMaxChatOptions options = MiniMaxChatOptions.builder()
                .model(MiniMaxApi.ChatModel.ABAB_6_5_S_Chat.getValue())
                // 使用 WebSearch 工具
                .tools(tools)
                .build();

        return chatModel.stream(new Prompt(new UserMessage(message), options))
                .map(response -> response.getResult().getOutput().getText());
    }
}
