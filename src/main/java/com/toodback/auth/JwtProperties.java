package com.toodback.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long expirationMillis;

    public String getSecret() {
        return secret;
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setExpirationMillis(long expirationMillis) {
        this.expirationMillis = expirationMillis;
    }
}