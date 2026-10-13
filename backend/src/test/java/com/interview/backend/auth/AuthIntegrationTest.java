package com.interview.backend.auth;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full integration test: real Spring context, real security filter chain,
 * real H2 database. Verifies the complete register -> login -> call
 * protected endpoint flow with a JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void fullAuthFlow_registerLoginAndAccessProtectedEndpoint() throws Exception {
        // register
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"alice@example.com","password":"secret123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());

        // login
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(
                loginResult.getResponse().getContentAsString(), "$.token");

        // protected endpoint with the token
        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(2)
    void protectedEndpoint_returns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    void login_returns401ForWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
