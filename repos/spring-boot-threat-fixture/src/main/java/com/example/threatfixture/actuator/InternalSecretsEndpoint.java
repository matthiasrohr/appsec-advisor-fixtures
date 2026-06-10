package com.example.threatfixture.actuator;

import java.util.List;
import java.util.Map;

import com.example.threatfixture.config.AppProperties;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "internalSecrets")
public class InternalSecretsEndpoint {
    private final JdbcTemplate jdbcTemplate;
    private final AppProperties appProperties;
    private final Environment environment;

    public InternalSecretsEndpoint(JdbcTemplate jdbcTemplate, AppProperties appProperties, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.appProperties = appProperties;
        this.environment = environment;
    }

    @ReadOperation
    public Map<String, Object> readInternalState() {
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT username AS \"username\", password AS \"password\", role AS \"role\" FROM users");
        List<Map<String, Object>> tokens = jdbcTemplate.queryForList(
                "SELECT owner AS \"owner\", provider AS \"provider\", token_value AS \"tokenValue\" FROM api_tokens");

        return Map.of(
                "adminBootstrapToken", appProperties.getAdminBootstrapToken(),
                "jwtSigningKey", appProperties.getJwtSigningKey(),
                "datasourceUrl", environment.getProperty("spring.datasource.url", ""),
                "datasourceUsername", environment.getProperty("spring.datasource.username", ""),
                "datasourcePassword", environment.getProperty("spring.datasource.password", ""),
                "users", users,
                "thirdPartyTokens", tokens);
    }

    @WriteOperation
    public Map<String, Object> overwriteBalance(Long accountId, Long balanceCents, String reason) {
        jdbcTemplate.update("UPDATE accounts SET balance_cents = ? WHERE id = ?", balanceCents, accountId);
        return Map.of(
                "status", "balance-overwritten",
                "accountId", accountId,
                "balanceCents", balanceCents,
                "reason", reason == null ? "not provided" : reason);
    }
}

