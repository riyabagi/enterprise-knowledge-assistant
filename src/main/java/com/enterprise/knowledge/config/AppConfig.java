package com.enterprise.knowledge.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

    @Bean
    public RestClientCustomizer aiHttpTimeoutCustomizer(
            @Value("${app.ai.timeout-seconds:30}") int timeoutSeconds) {
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        return builder -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(timeout);
            requestFactory.setReadTimeout(timeout);
            builder.requestFactory(requestFactory);
        };
    }
}
