package com.homehealthcare.auth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordPolicyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesConfiguredPasswordPolicyForSetupAndChangeFlows() throws Exception {
        mockMvc.perform(get("/api/auth/password-policy"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "minimumLength": 12,
                          "requireUppercase": true,
                          "requireLowercase": true,
                          "requireDigit": true,
                          "requireSymbol": true,
                          "commonPasswordCheckEnabled": true,
                          "preventReuseCount": 5,
                          "summary": "Password must be at least 12 characters and include upper, lower, digit, and symbol characters"
                        }
                        """));
    }
}
