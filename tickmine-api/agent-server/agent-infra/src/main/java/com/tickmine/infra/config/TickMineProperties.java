package com.tickmine.infra.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tickmine")
@Data
public class TickMineProperties {

    private Encryption encryption = new Encryption();
    private Map<String, ModelConfig> models = new HashMap<>();
    private Map<String, QuotaConfig> quota = new HashMap<>();
    private LlmKeys llm = new LlmKeys();
    private Auth auth = new Auth();

    @Data
    public static class Auth {
        /** Base64-encoded HMAC key (min 256 bits). Override via TICKMINE_JWT_SECRET in production. */
        private String jwtSecret =
                "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUYwMTIzNDU2Nzg5QUJDREVG";
        /** Access token lifetime in hours. */
        private long tokenTtlHours = 168;
    }

    @Data
    public static class Encryption {
        private String secretKey;
    }

    @Data
    public static class ModelConfig {
        private String provider;
        private String model;
        private String baseUrl;
    }

    @Data
    public static class QuotaConfig {
        private int dailyChatLimit;
    }

    @Data
    public static class LlmKeys {
        private String deepseekApiKey;
        private String qwenApiKey;
    }
}
