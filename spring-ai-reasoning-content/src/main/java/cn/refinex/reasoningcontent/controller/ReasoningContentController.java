package cn.refinex.reasoningcontent.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Objects;

/**
 * Reasoning Content Controller
 *
 * @author refinex
 */
@RestController
@RequestMapping("/reasoning")
public class ReasoningContentController {

    // 必须使用能返回 reasoning_content 的推理服务器
    private final DeepSeekChatModel deepSeekChatModel;

    private final ChatClient chatClient;

    /**
     * Constructor
     *
     * @param deepSeekChatModel DeepSeek Chat Model
     */
    public ReasoningContentController(DeepSeekChatModel deepSeekChatModel, ChatClient.Builder builder) {
        this.deepSeekChatModel = deepSeekChatModel;
        this.chatClient = builder.build();
    }

    @GetMapping("/compareSizesByChatModel")
    public String compareSizesByChatModel() {
        Prompt prompt = new Prompt("这两个数谁大：9.11 还是 9.8？");

        ChatResponse chatResponse = deepSeekChatModel.call(prompt);

        DeepSeekAssistantMessage deepSeekAssistantMessage = (DeepSeekAssistantMessage) chatResponse.getResult().getOutput();
        String reasoning = deepSeekAssistantMessage.getReasoningContent();
        String answer = deepSeekAssistantMessage.getText();

        return "推理内容: " + reasoning + ", \n\n 答案: " + answer;
    }

    @GetMapping("/compareSizesByChatClient")
    public String compareSizesByChatClient() {
        ChatResponse chatResponse = chatClient.prompt()
                .user("这两个数谁大：9.11 还是 9.8？")
                .call()
                .chatResponse();

        DeepSeekAssistantMessage deepSeekAssistantMessage = (DeepSeekAssistantMessage) chatResponse.getResult().getOutput();
        String reasoning = deepSeekAssistantMessage.getReasoningContent();
        String answer = deepSeekAssistantMessage.getText();

        return "推理内容: " + reasoning + ", \n\n 答案: " + answer;
    }

    @GetMapping(value = "/compareSizesByFlux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> compareSizesByFlux() {
        return chatClient.prompt()
                .user("这两个数谁大：9.11 还是 9.8？")
                .stream()
                .chatResponse()
                .map(chunk -> {
                    DeepSeekAssistantMessage msg = (DeepSeekAssistantMessage) chunk.getResult().getOutput();

                    // 优先输出推理内容
                    if (msg.getReasoningContent() != null) {
                        return "推理中: " + msg.getReasoningContent();
                    }

                    // 再输出最终回答
                    if (msg.getText() != null) {
                        return "答案: " + msg.getText();
                    }

                    return null;
                })
                .filter(Objects::nonNull);
    }

    @GetMapping(value = "/compareSizesByFluxV2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> compareSizesByFluxV2() {
        return chatClient.prompt()
                .user("这两个数谁大：9.11 还是 9.8？")
                .stream()
                .chatResponse()
                .map(chunk -> {
                    DeepSeekAssistantMessage msg = (DeepSeekAssistantMessage) chunk.getResult().getOutput();

                    if (msg.getReasoningContent() != null) {
                        return "event: reasoning\ndata: " + msg.getReasoningContent() + "\n\n";
                    }

                    if (msg.getText() != null) {
                        return "event: answer\ndata: " + msg.getText() + "\n\n";
                    }

                    return null;
                })
                .filter(Objects::nonNull);
    }

}
