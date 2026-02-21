package cn.refinex.prompt.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Prompt Controller
 *
 * @author refinex
 */
@Slf4j
@RestController
@RequestMapping("/prompt")
public class PromptController {

    /**
     * 通过 Spring Boot 自动配置注入的 ChatClient（默认使用 application.yml 中配置的模型）
     */
    private final ChatClient chatClient;

    /**
     * 从 classpath 加载系统提示词模板文件
     */
    @Value("classpath:/prompts/system-prompt.st")
    private Resource systemResource;

    /**
     * 从 classpath 加载用户提示词模板文件
     */
    @Value("classpath:/prompts/user-prompt.st")
    private Resource userResource;

    /**
     * 构造注入
     */
    public PromptController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 提示词模板——默认占位符
     * <p>
     * 演示：系统提示词用 {format} 控制输出格式，用户提示词用 {language} 指定编程语言。
     * ChatClient 内部会用 PromptTemplate + StTemplateRenderer 自动完成变量替换。
     *
     * @param format   输出格式，如 txt、markdown（默认 txt）
     * @param language 编程语言，如 Python、Java（默认 Python）
     */
    @GetMapping("/chat1")
    public String chat1(@RequestParam(value = "format", defaultValue = "txt") String format,
                        @RequestParam(value = "language", defaultValue = "Python") String language) {
        return chatClient.prompt()
                .system(s -> s
                        // 系统提示词模板：{format} 会被替换为实际的输出格式
                        .text("你是一个编程专家，精通各种编程语言，输出内容请用 {format} 格式。")
                        .param("format", format)
                )
                .user(u -> u
                        // 用户提示词模板：{language} 会被替换为实际的编程语言
                        .text("用 {language} 写一个冒泡排序算法")
                        .param("language", language)
                )
                .call()
                .content();
    }

    /**
     * 提示词模板——自定义占位符
     * <p>
     * 当提示词中包含 JSON 等花括号内容时，默认的 {} 占位符会产生冲突。
     * 此方法演示如何将定界符替换为 < >，避免与 JSON 语法冲突。
     *
     * @param format   输出格式（默认 txt）
     * @param language 编程语言（默认 Python）
     */
    @GetMapping("/chat2")
    public String chat2(@RequestParam(value = "format", defaultValue = "txt") String format,
                        @RequestParam(value = "language", defaultValue = "Python") String language) {
        // 1. 构建自定义模板渲染器：将定界符从 { } 改为 < >
        PromptTemplate.Builder templateBuilder = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder()
                        // 自定义开始定界符
                        .startDelimiterToken('<')
                        // 自定义结束定界符
                        .endDelimiterToken('>')
                        .build()
                );

        // 2. 渲染系统提示词（占位符变为 <format>）
        PromptTemplate systemPromptTemplate = templateBuilder
                .template("你是一个编程专家，精通各种编程语言，输出内容请用 <format> 格式。")
                .build();
        String systemPrompt = systemPromptTemplate.render(Map.of("format", format));

        // 3. 渲染用户提示词（占位符变为 <language>）
        PromptTemplate userPromptTemplate = templateBuilder
                .template("用 <language> 写一个冒泡排序算法")
                .build();
        String userPrompt = userPromptTemplate.render(Map.of("language", language));

        // 4. 用渲染后的纯字符串构建 Prompt 并调用
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    /**
     * 提示词模板——从资源文件加载
     * <p>
     * 适合提示词较长、需要独立维护、或按环境切换的场景。
     * 模板文件使用 .st 扩展名（StringTemplate 的约定），放在 classpath 下。
     *
     * @param format   输出格式（默认 txt）
     * @param language 编程语言（默认 Python）
     */
    @GetMapping("/chat3")
    public String chat3(@RequestParam(value = "format", defaultValue = "txt") String format,
                        @RequestParam(value = "language", defaultValue = "Python") String language) {

        // 1. 用资源文件创建系统提示词模板，渲染后生成 SystemMessage
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("format", format));

        // 2. 用资源文件创建用户提示词模板，渲染后生成 UserMessage
        PromptTemplate userPromptTemplate = new PromptTemplate(userResource);
        Message userMessage = userPromptTemplate.createMessage(Map.of("language", language));

        // 3. 组装 Prompt 并调用
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        return chatClient.prompt(prompt).call().content();
    }

    /**
     * 自定义模型参数
     * <p>
     * 演示如何在运行时覆盖全局默认的模型配置。
     * 这里切换了模型、限制了 maxTokens、调高了 temperature。
     *
     * @param message 用户输入的消息
     */
    @GetMapping("/chat4")
    public String chat4(@RequestParam(value = "message") String message) {
        // 1. 构建运行时模型参数（会覆盖 application.yml 中的默认值）
        DeepSeekChatOptions deepSeekChatOptions = DeepSeekChatOptions.builder()
                .model("deepseek-reasoner")    // 覆盖默认模型：deepseek-chat → deepseek-reasoner
                .maxTokens(200)                 // 限制最大输出 token 数（用于演示截断效果）
                .temperature(1.0)              // 覆盖默认温度：0.7 → 1.0（更高随机性）
                .build();

        // 2. 将用户消息和自定义参数一起打包成 Prompt
        //    Prompt 构造方法：new Prompt(String message, ChatOptions options)
        //    message 会被自动包装为 UserMessage
        Prompt prompt = new Prompt(message, deepSeekChatOptions);

        // 3. 通过 chatClient.prompt(Prompt) 发起调用
        //    运行时参数会与全局默认参数合并，冲突时运行时参数优先
        return chatClient.prompt(prompt).call().content();
    }

    @GetMapping("/chat5")
    public String chat5(@RequestParam(value = "message") String message) {
        return chatClient.prompt()
                .user(message)
                .options(DeepSeekChatOptions.builder()
                        .model("deepseek-reasoner")
                        .temperature(0.3)    // 代码生成场景用低温度
                        .build())
                .call()
                .content();
    }

}
