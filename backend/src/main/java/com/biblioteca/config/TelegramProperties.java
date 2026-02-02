package com.biblioteca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.telegram")
public class TelegramProperties {

    private String botToken;
    private String adminChatId;

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getAdminChatId() {
        return adminChatId;
    }

    public void setAdminChatId(String adminChatId) {
        this.adminChatId = adminChatId;
    }
}
