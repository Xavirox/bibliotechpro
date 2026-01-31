package com.biblioteca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.service")
public class AiServiceProperties {

    /**
     * URL del servicio de IA externo.
     * Por defecto: http://ai-service:8000/api/recomendar
     */
    private String url = "http://ai-service:8000/api/recomendar";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
