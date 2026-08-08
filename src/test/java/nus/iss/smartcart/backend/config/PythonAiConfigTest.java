package nus.iss.smartcart.backend.config;

// Author: Htet Nandar (Grace)

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PythonAiConfigTest {

    @Test
    void defaults_matchTheLocalDevelopmentPythonService() {
        PythonAiConfig config = new PythonAiConfig();

        assertEquals("http://localhost:8001", config.getBaseUrl());
        assertEquals(5, config.getConnectTimeoutSeconds());
        assertEquals(60, config.getReadTimeoutSeconds());
    }

    @Test
    void settersOverrideTheDefaults() {
        PythonAiConfig config = new PythonAiConfig();

        config.setBaseUrl("http://ai-service:9000");
        config.setConnectTimeoutSeconds(2);
        config.setReadTimeoutSeconds(30);

        assertEquals("http://ai-service:9000", config.getBaseUrl());
        assertEquals(2, config.getConnectTimeoutSeconds());
        assertEquals(30, config.getReadTimeoutSeconds());
    }
}
