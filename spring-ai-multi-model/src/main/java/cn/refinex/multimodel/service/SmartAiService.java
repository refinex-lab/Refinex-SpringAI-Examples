package cn.refinex.multimodel.service;

import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态路由AI服务
 *
 * @author refinex
 */
@Server
public class SmartAiService {

    /**
     * 聊天模型映射
     */
    private final Map<String, ChatModel> modelMap = new ConcurrentHashMap<>();

    /**
     * 构造方法注入
     *
     * @param openAi     openAi
     * @param deepSeek deepSeek
     */
    public SmartAiService(
            @Qualifier("openAiChatModel") ChatModel openAi,
            @Qualifier("deepSeekChatModel") ChatModel deepSeek) {
        modelMap.put("vip", openAi);
        modelMap.put("standard", deepSeek);
    }

    public String smartChat(String userId, String message) {
        // 1. 判断用户等级（示例逻辑）
        // String userLevel = userService.getUserLevel(userId);
        String userLevel = "VIP";

        // 2. 选择模型策略
        String strategyKey = "VIP".equals(userLevel) ? "vip" : "standard";
        ChatModel selectedModel = modelMap.get(strategyKey);

        // 3. 执行调用
        return selectedModel.call(message);
    }
}
