package com.example.threatfixture.web;

import java.util.List;
import java.util.Map;

import com.example.threatfixture.config.AppProperties;
import com.example.threatfixture.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final JdbcTemplate jdbcTemplate;
    private final AppProperties appProperties;
    private final AuditLogService auditLogService;

    public AdminController(JdbcTemplate jdbcTemplate, AppProperties appProperties, AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.appProperties = appProperties;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/settlements/override")
    public ResponseEntity<Map<String, Object>> overrideSettlement(
            @RequestHeader(name = "X-Admin-Token", required = false) String token,
            @RequestBody SettlementOverrideRequest request) {
        if (!appProperties.getAdminBootstrapToken().equals(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "denied"));
        }

        auditLogService.logAdminOverride(request.accountId, request.newBalanceCents, request.reason, token);
        jdbcTemplate.update("UPDATE accounts SET balance_cents = ? WHERE id = ?",
                request.newBalanceCents, request.accountId);
        return ResponseEntity.ok(Map.of("status", "updated", "accountId", request.accountId));
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(@RequestHeader(name = "X-Admin-Token", required = false) String token) {
        if (!appProperties.getAdminBootstrapToken().equals(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "denied"));
        }

        List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT username, password, role FROM users");
        return ResponseEntity.ok(users);
    }

    public static class SettlementOverrideRequest {
        public long accountId;
        public long newBalanceCents;
        public String reason;
    }
}

