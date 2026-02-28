package cn.refinex.chatclient.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 聊天控制器
 * <p>
 * WHY：Controller 只负责接收请求和返回响应。
 * 所有 AI 交互的默认配置（System Prompt、Memory、Logger）已在 ChatClientConfig 中定义。
 *
 * @author refinex
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    // 注入的是 ChatClient 接口，不是具体实现类
    private final ChatClient chatClient;

    /**
     * 聊天控制器构造函数注入
     *
     * @param generalChatClient 通用聊天客户端, 参数名称对应 ChatClientConfig#generalChatClient(ChatClient.Builder, ChatMemory)
     */
    public ChatController(ChatClient generalChatClient) {
        this.chatClient = generalChatClient;
    }

    /**
     * 同步对话
     * <p>
     * WHY 使用 conversationId：
     * Memory Advisor 通过 conversationId 区分不同对话。
     * 同一个 conversationId 的请求共享上下文。
     */
    @GetMapping("/ask")
    public Map<String, String> ask(
            @RequestParam String message,
            // 对话 ID，默认值为 "default" (见 ChatMemory.DEFAULT_CONVERSATION_ID)
            @RequestParam(defaultValue = "default") String conversationId) {
        String reply = chatClient.prompt()
                // 用户输入
                .user(message)
                // 通过 Advisor 参数传递 conversationId
                // chat_memory_conversation_id 也可以使用常量 ChatMemory.CONVERSATION_ID 替代
                //.advisors(spec -> spec.param("chat_memory_conversation_id", conversationId))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                // 标记执行模式为同步, 此时还没有触发 AI 调用
                .call()
                // 真正触发 AI 调用, 并返回回复内容
                .content();

        if (reply != null) {
            return Map.of("reply", reply);
        } else {
            return Map.of("error", "Empty reply");
        }
    }

    /**
     * 流式对话（SSE）
     * <p>
     * WHY 使用 stream()：
     * 流式响应让用户在 AI 生成过程中就能看到部分结果，
     * 大幅降低感知延迟。适合长回复场景。
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @RequestParam String message,
            // 对话 ID，默认值为 "default" (见 ChatMemory.DEFAULT_CONVERSATION_ID)
            @RequestParam(defaultValue = "default") String conversationId) {

        return chatClient.prompt()
                // 用户输入
                .user(message)
                // chat_memory_conversation_id 也可以使用常量 ChatMemory.CONVERSATION_ID 替代
                //.advisors(spec -> spec.param("chat_memory_conversation_id", conversationId))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                // 流式响应
                .stream()
                .content();
    }

    /**
     * 结构化输出 — 返回类型安全的 Java 对象
     * <p>
     * WHY 使用 entity()：
     * 直接将 AI 输出映射为 Java Record，
     * 避免手动 JSON 解析，获得编译时类型安全。
     */
    @GetMapping("/analyze")
    public CodeReview analyzeCode(@RequestParam String code) {
        return chatClient.prompt()
                .user("请分析以下代码的质量并给出改进建议：\n" + code)
                // 运行时覆盖 temperature —— 代码分析需要更低的随机性
                .options(ChatOptions.builder()
                        .temperature(0.2)
                        .build())
                .call()
                // 将 AI 输出映射为 CodeReview Record
                .entity(CodeReview.class);
    }

    /**
     * 代码审查结果 Record
     */
    record CodeReview(
            String summary,            // 总体评价
            int qualityScore,          // 质量评分 1-10
            List<String> issues,       // 发现的问题列表
            List<String> suggestions   // 改进建议列表
    ) {
    }
}
