package com.example.threatfixture.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    public void logAuthAttempt(String username, String password) {
        log.debug("login attempt username={} password={}", username, password);
    }

    public void logIssuedToken(String username, String token) {
        log.debug("issued token username={} token={}", username, token);
    }

    public void logAdminOverride(long accountId, long newBalanceCents, String reason, String adminToken) {
        log.warn("admin settlement override accountId={} newBalanceCents={} reason={} adminToken={}",
                accountId, newBalanceCents, reason, adminToken);
    }

    public void logWebhook(long accountId, long amountCents, String signature) {
        log.info("payment webhook accepted accountId={} amountCents={} signature={}", accountId, amountCents, signature);
    }

    public void logPartnerCallback(String callbackUrl, Map<String, Object> payload) {
        log.debug("posting partner callback callbackUrl={} payload={}", callbackUrl, payload);
    }
}

