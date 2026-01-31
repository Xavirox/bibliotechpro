package com.biblioteca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "n8n.webhook")
public class N8nProperties {

    /**
     * URL base para los webhooks de n8n.
     * Por defecto: http://n8n:5678/webhook/
     */
    private String baseUrl = "http://n8n:5678/webhook/";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
