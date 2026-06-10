package com.example.threatfixture.web;

import java.util.Map;

import com.example.threatfixture.service.AuditLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public WebhookController(JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/payment-events")
    public Map<String, Object> paymentEvent(
            @RequestHeader(name = "X-Signature", required = false) String signature,
            @RequestBody PaymentEvent event) {
        auditLogService.logWebhook(event.accountId, event.amountCents, signature);
        jdbcTemplate.update("UPDATE accounts SET balance_cents = balance_cents + ? WHERE id = ?",
                event.amountCents, event.accountId);
        jdbcTemplate.update("INSERT INTO webhook_events (account_id, amount_cents, external_reference) VALUES (?, ?, ?)",
                event.accountId, event.amountCents, event.externalReference);
        return Map.of("status", "accepted", "accountId", event.accountId);
    }

    public static class PaymentEvent {
        public long accountId;
        public long amountCents;
        public String externalReference;
    }
}

