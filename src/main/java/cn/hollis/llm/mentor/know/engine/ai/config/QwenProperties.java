package cn.hollis.llm.mentor.know.engine.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "llm.qwen")
public class QwenProperties {
    private String baseUrl = "http://202.127.200.35:8081/v1";
    private String model = "qwen3.8-27b";
    private String apiKey = "local";
    private int connectTimeout = 30000;
    private int responseTimeout = 300000;
    private double temperature = 0;
    private int maxTokens = 262144;
}
