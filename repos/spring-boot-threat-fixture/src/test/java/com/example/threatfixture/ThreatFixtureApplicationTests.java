package com.example.threatfixture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ThreatFixtureApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginAndCrossAccountReadAreReachableWithoutSession() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));

        mockMvc.perform(get("/api/accounts/1002").param("viewer", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("bob"));
    }

    @Test
    void actuatorIsReachableWithoutCredentials() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void unsignedWebhookMutatesBalance() throws Exception {
        mockMvc.perform(post("/api/webhooks/payment-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":1001,\"amountCents\":5000,\"externalReference\":\"test-webhook\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));
    }

    @Test
    void oauthImplicitGrantRedirectsTokenToSpaFragment() throws Exception {
        mockMvc.perform(get("/oauth/authorize")
                        .param("response_type", "token")
                        .param("client_id", "wallet-spa")
                        .param("redirect_uri", "http://localhost:8080/oauth-callback.html")
                        .param("scope", "accounts:read admin:read")
                        .param("state", "test-state")
                        .param("login_hint", "alice"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/oauth-callback.html#access_token=")))
                .andExpect(header().string("Location", containsString("token_type=Bearer")));
    }

    @Test
    void missingAndBrokenAccessControlsAreReachable() throws Exception {
        mockMvc.perform(get("/api/profile/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.storedPassword").value("password123"));

        mockMvc.perform(get("/api/reports/admin-summary").param("supportOverride", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].password", notNullValue()));
    }

    @Test
    void dangerousActuatorEndpointsAreExposedWithoutCredentials() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.internalSecrets.href", notNullValue()))
                .andExpect(jsonPath("$._links.env.href", notNullValue()))
                .andExpect(jsonPath("$._links.shutdown.href", notNullValue()));

        mockMvc.perform(get("/actuator/internalSecrets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminBootstrapToken").value("adm1n-demo-token-please-change"))
                .andExpect(jsonPath("$.jwtSigningKey").value("fixture-jwt-signing-key-too-short"))
                .andExpect(jsonPath("$.users[0].password", notNullValue()))
                .andExpect(jsonPath("$.thirdPartyTokens[0].tokenValue", notNullValue()));
    }
}
