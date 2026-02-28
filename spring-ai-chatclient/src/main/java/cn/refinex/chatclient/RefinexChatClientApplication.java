package cn.refinex.chatclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ChatClient Example Application
 *
 * @author refinex
 */
@SpringBootApplication
public class RefinexChatClientApplication {

    private RefinexChatClientApplication() {
        /* This utility class should not be instantiated */
    }

    static void main() {
        SpringApplication.run(RefinexChatClientApplication.class);
    }
}
