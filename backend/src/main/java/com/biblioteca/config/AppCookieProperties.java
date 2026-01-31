package com.biblioteca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "app.cookie")
public class AppCookieProperties {

    /**
     * Si la cookie debe ser Secure (solo HTTPS).
     * En producción debe ser true.
     */
    private boolean secure = false;

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }
}
