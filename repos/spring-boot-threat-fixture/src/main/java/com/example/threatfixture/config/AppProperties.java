package com.example.threatfixture.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String adminBootstrapToken;
    private String jwtSigningKey;
    private String oauthClientId;
    private String oauthAllowedRedirectPattern;
    private long oauthTokenTtlSeconds;
    private String uploadRoot;
    private int partnerCallbackTimeoutMs;
    private boolean callbackAllowPrivateNetworks;

    public String getAdminBootstrapToken() {
        return adminBootstrapToken;
    }

    public void setAdminBootstrapToken(String adminBootstrapToken) {
        this.adminBootstrapToken = adminBootstrapToken;
    }

    public String getJwtSigningKey() {
        return jwtSigningKey;
    }

    public void setJwtSigningKey(String jwtSigningKey) {
        this.jwtSigningKey = jwtSigningKey;
    }

    public String getOauthClientId() {
        return oauthClientId;
    }

    public void setOauthClientId(String oauthClientId) {
        this.oauthClientId = oauthClientId;
    }

    public String getOauthAllowedRedirectPattern() {
        return oauthAllowedRedirectPattern;
    }

    public void setOauthAllowedRedirectPattern(String oauthAllowedRedirectPattern) {
        this.oauthAllowedRedirectPattern = oauthAllowedRedirectPattern;
    }

    public long getOauthTokenTtlSeconds() {
        return oauthTokenTtlSeconds;
    }

    public void setOauthTokenTtlSeconds(long oauthTokenTtlSeconds) {
        this.oauthTokenTtlSeconds = oauthTokenTtlSeconds;
    }

    public String getUploadRoot() {
        return uploadRoot;
    }

    public void setUploadRoot(String uploadRoot) {
        this.uploadRoot = uploadRoot;
    }

    public int getPartnerCallbackTimeoutMs() {
        return partnerCallbackTimeoutMs;
    }

    public void setPartnerCallbackTimeoutMs(int partnerCallbackTimeoutMs) {
        this.partnerCallbackTimeoutMs = partnerCallbackTimeoutMs;
    }

    public boolean isCallbackAllowPrivateNetworks() {
        return callbackAllowPrivateNetworks;
    }

    public void setCallbackAllowPrivateNetworks(boolean callbackAllowPrivateNetworks) {
        this.callbackAllowPrivateNetworks = callbackAllowPrivateNetworks;
    }
}
