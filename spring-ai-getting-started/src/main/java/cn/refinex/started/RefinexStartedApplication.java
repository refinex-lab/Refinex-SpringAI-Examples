package cn.refinex.started;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI 快速上手示例应用
 *
 * @author refinex
 */
@SpringBootApplication
public class RefinexStartedApplication {

    private RefinexStartedApplication() {
        /* This utility class should not be instantiated */
    }

    static void main(String[] args) {
        SpringApplication.run(RefinexStartedApplication.class, args);
    }
}
