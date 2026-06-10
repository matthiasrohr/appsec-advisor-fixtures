package com.example.threatfixture.web;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final JdbcTemplate jdbcTemplate;

    public AccountController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/{accountId}")
    public Map<String, Object> getAccount(@PathVariable long accountId,
            @RequestParam(defaultValue = "anonymous") String viewer) {
        Map<String, Object> account = jdbcTemplate.queryForMap(
                "SELECT id AS \"id\", owner AS \"owner\", email AS \"email\", balance_cents AS \"balanceCents\" FROM accounts WHERE id = ?",
                accountId);
        account.put("requestedBy", viewer);
        return account;
    }

    @GetMapping("/search")
    public List<Map<String, Object>> searchAccounts(@RequestParam String owner) {
        String sql = "SELECT id AS \"id\", owner AS \"owner\", email AS \"email\", balance_cents AS \"balanceCents\" FROM accounts WHERE owner LIKE '%" + owner + "%'";
        return jdbcTemplate.queryForList(sql);
    }

    @PostMapping("/documents/search")
    public Map<String, Object> searchDocuments(@RequestBody Map<String, Object> filter) {
        String mongoQuery = "{ collection: 'accounts', filter: " + filter + ", $where: '" + filter.get("$where") + "' }";
        return Map.of("engine", "MongoDB", "operation", "find", "rawFilter", filter, "mongoQuery", mongoQuery);
    }

    @PutMapping("/{accountId}/email")
    public Map<String, Object> updateEmail(@PathVariable long accountId,
            @RequestParam(defaultValue = "anonymous") String viewer,
            @RequestBody EmailChangeRequest request) {
        jdbcTemplate.update("UPDATE accounts SET email = ? WHERE id = ?", request.email, accountId);
        return jdbcTemplate.queryForMap(
                "SELECT id AS \"id\", owner AS \"owner\", email AS \"email\", balance_cents AS \"balanceCents\", '" + viewer + "' AS \"updatedBy\" FROM accounts WHERE id = ?",
                accountId);
    }

    public static class EmailChangeRequest {
        public String email;
    }
}
