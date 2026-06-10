package com.example.threatfixture.web;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.example.threatfixture.config.AppProperties;
import com.example.threatfixture.service.AuditLogService;
import com.example.threatfixture.service.DemoTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth")
public class OAuthController {
    private final AppProperties appProperties;
    private final DemoTokenService demoTokenService;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public OAuthController(AppProperties appProperties, DemoTokenService demoTokenService,
            JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.appProperties = appProperties;
        this.demoTokenService = demoTokenService;
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(
            @RequestParam(name = "response_type") String responseType,
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "redirect_uri") String redirectUri,
            @RequestParam(defaultValue = "") String scope,
            @RequestParam(defaultValue = "") String state,
            @RequestParam(name = "login_hint", defaultValue = "alice") String loginHint) {
        if (!"token".equals(responseType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "unsupported_response_type"));
        }
        if (!appProperties.getOauthClientId().equals(clientId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unknown_client"));
        }
        if (!"*".equals(appProperties.getOauthAllowedRedirectPattern())
                && !redirectUri.startsWith(appProperties.getOauthAllowedRedirectPattern())) {
            return ResponseEntity.badRequest().body(Map.of("error", "redirect_uri_not_allowed"));
        }

        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT username AS \"username\", role AS \"role\" FROM users WHERE username = ?", loginHint);
        String token = demoTokenService.createToken(
                user.get("username").toString(),
                user.get("role").toString(),
                "oauth-implicit");
        auditLogService.logIssuedToken(user.get("username").toString(), token);

        String fragment = "access_token=" + url(token)
                + "&token_type=Bearer"
                + "&expires_in=" + appProperties.getOauthTokenTtlSeconds()
                + "&scope=" + url(scope)
                + "&state=" + url(state);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirectUri + "#" + fragment));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/userinfo")
    public Map<String, Object> userInfo(@RequestHeader(name = "Authorization", required = false) String authorization) {
        Map<String, Object> claims = demoTokenService.readClaimsWithoutVerifyingSignature(authorization);
        String username = claims.getOrDefault("sub", "anonymous").toString();
        if ("anonymous".equals(username)) {
            return Map.of("sub", "anonymous", "authenticated", false);
        }
        Map<String, Object> profile = jdbcTemplate.queryForMap(
                "SELECT username AS \"sub\", role AS \"role\" FROM users WHERE username = ?", username);
        profile.put("authenticated", true);
        return profile;
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

