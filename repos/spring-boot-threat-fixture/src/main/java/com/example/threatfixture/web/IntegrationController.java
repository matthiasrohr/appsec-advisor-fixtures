package com.example.threatfixture.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import com.example.threatfixture.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {
    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public IntegrationController(RestTemplate restTemplate, JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.restTemplate = restTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/fetch-preview")
    public ResponseEntity<Map<String, Object>> fetchPreview(@RequestParam String url) {
        URI target = URI.create(url);
        if (!target.getScheme().equals("http") && !target.getScheme().equals("https")) {
            return ResponseEntity.badRequest().body(Map.of("error", "unsupported scheme"));
        }

        ResponseEntity<String> response = restTemplate.getForEntity(target, String.class);
        String body = response.getBody() == null ? "" : response.getBody();
        return ResponseEntity.ok(Map.of(
                "source", target.toString(),
                "status", response.getStatusCodeValue(),
                "preview", body.substring(0, Math.min(500, body.length()))));
    }

    @PostMapping("/notify-partner")
    public ResponseEntity<Map<String, Object>> notifyPartner(@RequestBody PartnerCallbackRequest request) {
        Map<String, Object> account = jdbcTemplate.queryForMap(
                "SELECT id AS \"id\", owner AS \"owner\", email AS \"email\", balance_cents AS \"balanceCents\" FROM accounts WHERE id = ?",
                request.accountId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("account", account);
        payload.put("event", request.event);
        payload.put("note", request.note);

        auditLogService.logPartnerCallback(request.callbackUrl, payload);
        ResponseEntity<String> callbackResponse = restTemplate.postForEntity(request.callbackUrl, payload, String.class);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "callbackUrl", request.callbackUrl,
                "callbackStatus", callbackResponse.getStatusCodeValue()));
    }

    public static class PartnerCallbackRequest {
        public String callbackUrl;
        public long accountId;
        public String event;
        public String note;
    }
}
