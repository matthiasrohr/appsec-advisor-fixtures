package com.example.threatfixture.web;

import java.util.List;
import java.util.Map;

import com.example.threatfixture.service.AuditLogService;
import com.example.threatfixture.service.DemoTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;
    private final DemoTokenService demoTokenService;

    public AuthController(JdbcTemplate jdbcTemplate, AuditLogService auditLogService, DemoTokenService demoTokenService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
        this.demoTokenService = demoTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        auditLogService.logAuthAttempt(request.username, request.password);

        String sql = "SELECT username AS \"username\", role AS \"role\" FROM users WHERE username = '" + request.username
                + "' AND password = '" + request.password + "'";
        List<Map<String, Object>> matches = jdbcTemplate.queryForList(sql);
        if (matches.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "invalid"));
        }

        Map<String, Object> user = matches.get(0);
        String token = demoTokenService.createToken(
                user.get("username").toString(),
                user.get("role").toString(),
                "password");
        auditLogService.logIssuedToken(user.get("username").toString(), token);
        return ResponseEntity.ok(Map.of("token", token, "role", user.get("role"), "expiresInSeconds", 86400));
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }
}
