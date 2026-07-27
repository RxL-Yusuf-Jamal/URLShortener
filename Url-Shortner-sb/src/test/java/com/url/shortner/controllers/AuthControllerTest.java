package com.url.shortner.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.shortner.dtos.LoginRequest;
import com.url.shortner.dtos.RegisterRequest;
import com.url.shortner.exceptions.DuplicateResourceException;
import com.url.shortner.security.jwt.JwtAuthenticationFilter;
import com.url.shortner.security.jwt.JwtAuthenticationResponse;
import com.url.shortner.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AuthController's endpoints are public via a URL-pattern rule in the real WebSecurityConfig
// (not loaded in this slice), not @PreAuthorize, so the security filter chain is irrelevant here.
@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void register_withValidPayload_returns200() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123", Set.of("ROLE_USER"));

        mockMvc.perform(post("/api/auth/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered Successfully"));

        verify(userService).registerUser(any());
    }

    @Test
    void register_withDuplicateUsername_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123", Set.of("ROLE_USER"));
        doThrow(new DuplicateResourceException("Username already exists")).when(userService).registerUser(any());

        mockMvc.perform(post("/api/auth/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void register_withBlankUsername_returns400WithFieldError() throws Exception {
        RegisterRequest request = new RegisterRequest("", "alice@example.com", "password123", Set.of("ROLE_USER"));

        mockMvc.perform(post("/api/auth/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void register_withInvalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "not-an-email", "password123", Set.of("ROLE_USER"));

        mockMvc.perform(post("/api/auth/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        LoginRequest request = new LoginRequest("alice", "password123");
        when(userService.authenticateUser(any())).thenReturn(new JwtAuthenticationResponse("jwt-token"));

        mockMvc.perform(post("/api/auth/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_withBlankPassword_returns400() throws Exception {
        LoginRequest request = new LoginRequest("alice", "");

        mockMvc.perform(post("/api/auth/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
