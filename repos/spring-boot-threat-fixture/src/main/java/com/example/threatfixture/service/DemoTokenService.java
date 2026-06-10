package com.example.threatfixture.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.example.threatfixture.config.AppProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class DemoTokenService {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AppProperties appProperties;

    public DemoTokenService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String createToken(String username, String role, String authMethod) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", username);
        payload.put("role", role);
        payload.put("amr", authMethod);
        payload.put("iat", Instant.now().getEpochSecond());
        payload.put("exp", Instant.now().getEpochSecond() + appProperties.getOauthTokenTtlSeconds());

        String signingInput = base64Json(header) + "." + base64Json(payload);
        return signingInput + "." + hmac(signingInput);
    }

    public Optional<String> extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return Optional.of(authorizationHeader.substring("Bearer ".length()));
    }

    public Map<String, Object> readClaimsWithoutVerifyingSignature(String authorizationHeader) {
        return extractBearerToken(authorizationHeader)
                .map(this::readClaimsWithoutVerifyingSignatureFromToken)
                .orElseGet(Map::of);
    }

    public Map<String, Object> readClaimsWithoutVerifyingSignatureFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return Map.of();
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return MAPPER.readValue(decoded, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String base64Json(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MAPPER.writeValueAsBytes(value));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode token", e);
        }
    }

    private String hmac(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appProperties.getJwtSigningKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign token", e);
        }
    }
}

