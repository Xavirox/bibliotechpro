package com.biblioteca.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
@ConfigurationProperties(prefix = "gemini.api")
public class GeminiProperties {

    private static final Logger logger = LoggerFactory.getLogger(GeminiProperties.class);

    private String key;

    @PostConstruct
    public void validateKey() {
        if (key == null || key.isBlank()) {
            logger.warn("⚠️ GEMINI API KEY IS NOT CONFIGURED! AI recommendations will fail.");
        } else {
            logger.info("✅ Gemini API key is configured ({}...)", key.substring(0, Math.min(8, key.length())));
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
