package cn.hollis.llm.mentor.know.engine.ai.config;

import dev.langchain4j.http.client.spring.restclient.SpringRestClientBuilder;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

/** 问答、意图识别、标题、图片理解共享模型配置；向量模型单独配置。 */
@Configuration
@EnableConfigurationProperties(QwenProperties.class)
public class QwenConfiguration {
    private SpringRestClientBuilder httpClient(QwenProperties p) {
        return new SpringRestClientBuilder()
                .connectTimeout(Duration.ofMillis(p.getConnectTimeout()))
                .readTimeout(Duration.ofMillis(p.getResponseTimeout()));
    }

    private Map<String, Object> parameters() {
        // llama.cpp 使用 chat_template_kwargs 接收思考开关。
        return Map.of("chat_template_kwargs", Map.of("enable_thinking", false));
    }

    @Bean
    public OpenAiChatModel openAiChatModel(QwenProperties p) {
        return OpenAiChatModel.builder()
                .httpClientBuilder(httpClient(p))
                .baseUrl(p.getBaseUrl()).apiKey(p.getApiKey()).modelName(p.getModel())
                .temperature(p.getTemperature()).maxTokens(p.getMaxTokens())
                .timeout(Duration.ofMillis(p.getResponseTimeout())).maxRetries(1)
                .customParameters(parameters()).build();
    }

    @Bean
    public OpenAiStreamingChatModel openAiStreamingChatModel(QwenProperties p) {
        return OpenAiStreamingChatModel.builder()
                .httpClientBuilder(httpClient(p))
                .baseUrl(p.getBaseUrl()).apiKey(p.getApiKey()).modelName(p.getModel())
                .temperature(p.getTemperature()).maxTokens(p.getMaxTokens())
                .timeout(Duration.ofMillis(p.getResponseTimeout()))
                .customParameters(parameters()).build();
    }
}
