package cn.refinex.ollama.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Ollama 聊天 Controller
 *
 * @author refinex
 */
@Slf4j
@RestController
@RequestMapping("/ollama")
@RequiredArgsConstructor
public class OllamaController {

    /**
     * 按名称注入，避免与其他 ChatClient Bean 冲突
     */
    @Resource(name = "ollamaChatClient")
    private ChatClient chatClient;

    /**
     * 直接注入 OllamaChatModel（底层调用，适合需要访问原始 API 参数的场景）
     */
    private final OllamaChatModel chatModel;

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
     * 运行时覆盖模型参数（适合 A/B 测试或临时切换模型）
     */
    @GetMapping("/chat-custom")
    public String chatWithCustomOptions(@RequestParam(defaultValue = "生成 5 个海盗名字") String message) {
        ChatResponse response = chatModel.call(
                new Prompt(
                        message,
                        OllamaChatOptions.builder()
                                .model(OllamaModel.LLAMA3_1)   // 枚举常量，避免硬编码字符串
                                .temperature(0.4)
                                .build()
                )
        );
        return response.getResult().getOutput().getText();
    }

    /**
     * 工具方法，供 AI 调用
     */
    @Tool(description = "根据城市名查询当前天气")
    public String getWeather(String city) {
        // return weatherService.query(city);
        return "晴天";
    }

    /**
     * 调用工具方法
     */
    @GetMapping("/search-weather")
    public String searchWeather(@RequestParam(defaultValue = "北京今天天气怎么样？") String message) {
        return chatClient.prompt(message)
                .tools(this) // 注入天气查询工具
                .call()
                .content();
    }

    /**
     * 开启思维链
     */
    @GetMapping("/thinking-chain")
    public String thinkingChain(@RequestParam(defaultValue = "请详细介绍一下 Java 语言") String message) {
        // 开启思维链（适合复杂推理场景）
        ChatResponse response = chatModel.call(
                new Prompt(
                        "strawberry 这个单词里有几个字母 r？",
                        OllamaChatOptions.builder()
                                // Ollama 0.12+ 对支持思维链的模型（如 Qwen3、DeepSeek-R1、DeepSeek-v3.1、gpt-oss）默认自动开启 Thinking 模式
                                .model("qwen3")
                                // 手动开启思维链
                                .enableThinking()
                                // 关闭思维链（适合简单对话，节省延迟和算力）
                                // .disableThinking()
                                .build()
                )
        );

        // 读取推理过程
        String thinking = response.getResult().getMetadata().get("thinking");
        String answer = response.getResult().getOutput().getText();

        return "推理过程: " + thinking + ", \n\n 答案: " + answer;
    }

    /**
     * 开启思维链 （Stream）
     */
    @GetMapping(value = "/thinking-chain-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> thinkingChainStream(@RequestParam(defaultValue = "请详细介绍一下 Java 语言") String message) {
        Flux<ChatResponse> stream = chatModel.stream(
                new Prompt(
                        message,
                        OllamaChatOptions.builder()
                                // Ollama 0.12+ 对支持思维链的模型（如 Qwen3、DeepSeek-R1、DeepSeek-v3.1、gpt-oss）默认自动开启 Thinking 模式
                                .model("qwen3")
                                // 手动开启思维链
                                .enableThinking()
                                // 关闭思维链（适合简单对话，节省延迟和算力）
                                // .disableThinking()
                                .build()
                )
        );

        return stream.flatMap(chunk -> {
            // 读取推理过程
            String thinking = chunk.getResult().getMetadata().get("thinking");
            String content = chunk.getResult().getOutput().getText();

            List<String> events = new ArrayList<>(2);
            if (thinking != null && !thinking.isEmpty()) {
                events.add("[思考] " + thinking);
            }
            if (content != null && !content.isEmpty()) {
                events.add(content);
            }
            return Flux.fromIterable(events);
        });
    }

    /**
     * 结构化输出（JSON Schema）示例
     */
    @GetMapping("/structured-output")
    public MathReasoning structuredOutput() {
        var outputConverter = new BeanOutputConverter<>(MathReasoning.class);

        Prompt prompt = new Prompt(
                "如何解方程 8x + 7 = -23？",
                OllamaChatOptions.builder()
                        .model("llama3.2")
                        // 自动反序列化
                        .format(outputConverter.getJsonSchemaMap())
                        .build()
        );
        ChatResponse response = chatModel.call(prompt);
        return outputConverter.convert(Objects.requireNonNull(response.getResult().getOutput().getText()));
    }

    // 不可省略 @JsonProperty(required = true)：省略后生成的 Schema 不会标记字段为必填，模型可能输出不完整的 JSON，导致反序列化失败。
    public record MathReasoning(
            @JsonProperty(required = true) Steps steps,
            @JsonProperty(required = true, value = "final_answer") String finalAnswer
    ) {
        public record Steps(@JsonProperty(required = true) Items[] items) {
            public record Items(String explanation, String output) {
            }
        }
    }

}
