package nus.iss.smartcart.backend.config;

// Author: Htet Nandar (Grace)

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Typed config for calling smartcart-ai-service (the Python/FastAPI AI microservice
 * nested under this project - see /smartcart-ai-service). See PythonAiChatService /
 * PythonAiRecommendationService, and application.properties for the actual values.
 */
@Configuration
@ConfigurationProperties(prefix = "ai.python-service")
public class PythonAiConfig {

    private String baseUrl = "http://localhost:8001";
    private int connectTimeoutSeconds = 5;
    private int readTimeoutSeconds = 60;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }

    public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
    public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }
}
