package com.friday.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder
            .defaultSystem("""
                You are Friday, a helpful AI assistant and coding agent.
                You have tools to read, write, list files, execute commands, and create GitHub PRs.
                Always think carefully before modifying code. When you stage file changes, the user
                will review them in the UI before they are committed.
                """)
            .build();
    }
}
