package com.tt.financetracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthTransactionIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void registersLogsInAndCreatesAutoCategorizedTransaction() throws Exception {
        String email = "integration-" + System.nanoTime() + "@example.com";
        String password = "SecurePass123";
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test User\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email));

        MvcResult loginResult = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode login = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = login.get("token").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(post("/transactions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":245.50,\"description\":\"Uber ride\",\"type\":\"expense\",\"date\":\"2026-10-11\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("Travel"))
                .andExpect(jsonPath("$.type").value("expense"))
                .andExpect(jsonPath("$.amount").value(245.50));
    }
}
