package cn.refinex.multimodel.controller;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多模型集成 Controller
 *
 * @author refinex
 */
@RestController
@RequestMapping("/multi-model")
public class MultiModelController {

    private final ChatModel openAiChatModel;
    private final ChatModel deepSeekChatModel;

    public MultiModelController(
            @Qualifier("openAiChatModel") ChatModel openAiChatModel,
            @Qualifier("deepSeekChatModel") ChatModel deepSeekChatModel) {
        this.openAiChatModel = openAiChatModel;
        this.deepSeekChatModel = deepSeekChatModel;
    }

    @GetMapping("/openai")
    public String talkToGpt(@RequestParam("msg") String msg) {
        return openAiChatModel.call(msg);
    }

    @GetMapping("deepseek")
    public String talkToDeepSeek(@RequestParam("msg") String msg) {
        return deepSeekChatModel.call(msg);
    }
}
