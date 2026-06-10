package com.example.threatfixture.web;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tokens")
public class TokenVaultController {
    private final JdbcTemplate jdbcTemplate;

    public TokenVaultController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    public Map<String, Object> storeToken(@RequestBody StoreTokenRequest request) {
        jdbcTemplate.update("INSERT INTO api_tokens (owner, provider, token_value) VALUES (?, ?, ?)",
                request.owner, request.provider, request.tokenValue);
        return Map.of("status", "stored", "owner", request.owner, "provider", request.provider);
    }

    @GetMapping("/{owner}")
    public List<Map<String, Object>> listTokens(@PathVariable String owner) {
        return jdbcTemplate.queryForList(
                "SELECT id, owner, provider, token_value FROM api_tokens WHERE owner = ?", owner);
    }

    public static class StoreTokenRequest {
        public String owner;
        public String provider;
        public String tokenValue;
    }
}

