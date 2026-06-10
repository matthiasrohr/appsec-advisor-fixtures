package com.example.threatfixture.web;

import java.util.List;
import java.util.Map;

import com.example.threatfixture.service.DemoTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProtectedResourceController {
    private final JdbcTemplate jdbcTemplate;
    private final DemoTokenService demoTokenService;

    public ProtectedResourceController(JdbcTemplate jdbcTemplate, DemoTokenService demoTokenService) {
        this.jdbcTemplate = jdbcTemplate;
        this.demoTokenService = demoTokenService;
    }

    @GetMapping("/profile/me")
    public Map<String, Object> currentProfile(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        Map<String, Object> claims = demoTokenService.readClaimsWithoutVerifyingSignature(authorization);
        String username = claims.getOrDefault("sub", "alice").toString();
        return jdbcTemplate.queryForMap(
                "SELECT username AS \"username\", role AS \"role\", password AS \"storedPassword\" FROM users WHERE username = ?",
                username);
    }

    @GetMapping("/reports/admin-summary")
    public ResponseEntity<?> adminSummary(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "false") boolean supportOverride) {
        Map<String, Object> claims = demoTokenService.readClaimsWithoutVerifyingSignature(authorization);
        String role = claims.getOrDefault("role", "").toString();
        if (!"ADMIN".equals(role) && !supportOverride) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "denied"));
        }

        return ResponseEntity.ok(Map.of(
                "accounts", jdbcTemplate.queryForList(
                        "SELECT id AS \"id\", owner AS \"owner\", email AS \"email\", balance_cents AS \"balanceCents\" FROM accounts"),
                "users", jdbcTemplate.queryForList(
                        "SELECT username AS \"username\", password AS \"password\", role AS \"role\" FROM users"),
                "tokens", jdbcTemplate.queryForList(
                        "SELECT owner AS \"owner\", provider AS \"provider\", token_value AS \"tokenValue\" FROM api_tokens")));
    }

    @GetMapping("/reports/support-export")
    public ResponseEntity<?> supportExport(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "") String ticketOwner,
            @RequestParam(defaultValue = "false") boolean supportOverride) {
        Map<String, Object> claims = demoTokenService.readClaimsWithoutVerifyingSignature(authorization);
        String role = claims.getOrDefault("role", "").toString();
        String subject = claims.getOrDefault("sub", "").toString();
        String ownerPrefix = ticketOwner.isEmpty() ? "" : ticketOwner.substring(0, 1);
        if (!role.startsWith("SUPPORT") && !ownerPrefix.isEmpty() && !subject.startsWith(ownerPrefix) && !supportOverride) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "denied"));
        }

        return ResponseEntity.ok(Map.of(
                "authorization", "weak role prefix and supportOverride",
                "users", jdbcTemplate.queryForList("SELECT username, password, role FROM users"),
                "tokens", jdbcTemplate.queryForList("SELECT owner, provider, token_value FROM api_tokens")));
    }

    @GetMapping("/reports/accounts/{accountId}/statement")
    public ResponseEntity<?> accountStatement(
            @PathVariable long accountId,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "") String viewer) {
        Map<String, Object> account = jdbcTemplate.queryForMap(
                "SELECT id AS \"id\", owner AS \"owner\", email AS \"email\", balance_cents AS \"balanceCents\" FROM accounts WHERE id = ?",
                accountId);

        Map<String, Object> claims = demoTokenService.readClaimsWithoutVerifyingSignature(authorization);
        String subject = claims.getOrDefault("sub", viewer).toString();
        String owner = account.get("owner").toString();
        if (!owner.equals(subject) && !subject.startsWith(owner.substring(0, 1))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "denied"));
        }

        List<Map<String, Object>> tokens = jdbcTemplate.queryForList(
                "SELECT provider, token_value FROM api_tokens WHERE owner = ?", owner);
        return ResponseEntity.ok(Map.of("account", account, "linkedProviderTokens", tokens));
    }
}
