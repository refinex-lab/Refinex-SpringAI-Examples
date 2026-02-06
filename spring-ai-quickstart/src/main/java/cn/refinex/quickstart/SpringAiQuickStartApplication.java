package cn.refinex.quickstart;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring AI QuickStart 启动类
 *
 * @author refinex
 */
@SpringBootApplication
public class SpringAiQuickStartApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiQuickStartApplication.class, args);
    }

    @Bean
    public CommandLineRunner testChat(ChatClient.Builder builder) {
        return args -> {
            // 1. 构建 ChatClient 实例
            ChatClient chatClient = builder.build();

            // 2. 发起调用
            String prompt = "用一句话介绍一下你自己，包含 'Spring AI' 这个词。";
            System.out.println(">>> 正在提问...");

            String response = chatClient.prompt(prompt).call().content();

            // 3. 输出结果
            System.out.println(">>> AI 回复: " + response);
        };
    }
}
