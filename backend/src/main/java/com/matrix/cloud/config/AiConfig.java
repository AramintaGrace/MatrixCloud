package com.matrix.cloud.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.*;

@Configuration
public class AiConfig {

    @Value("${langchain4j.open-ai.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.model-name:deepseek-chat}")
    private String modelName;

    @Value("${langchain4j.open-ai.temperature:0.3}")
    private Double temperature;

    @Value("${langchain4j.open-ai.timeout:180s}")
    private Duration timeout;

    @Value("${langchain4j.open-ai.max-tokens:4096}")
    private Integer maxTokens;

    @Value("${langchain4j.open-ai.log-requests:true}")
    private Boolean logRequests;

    @Value("${langchain4j.open-ai.log-responses:true}")
    private Boolean logResponses;

    @Value("${meeting.summary.thread-pool.core-size:4}")
    private int corePoolSize;

    @Value("${meeting.summary.thread-pool.max-size:8}")
    private int maxPoolSize;

    @Value("${meeting.summary.thread-pool.queue-capacity:100}")
    private int queueCapacity;

    @Value("${meeting.summary.thread-pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(timeout)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }

    @Bean(name = "meetingSummaryExecutor")
    public ExecutorService meetingSummaryExecutor() {
        return new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            keepAliveSeconds,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            r -> {
                Thread t = new Thread(r, "meeting-summary-" + System.currentTimeMillis() % 10000);
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
