package com.url.shortner.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.shortner.dtos.LoginRequest;
import com.url.shortner.dtos.RegisterRequest;
import com.url.shortner.exceptions.DuplicateResourceException;
import com.url.shortner.exceptions.RateLimitExceededException;
import org.springframework.security.authentication.BadCredentialsException;
import com.url.shortner.security.jwt.JwtAuthenticationFilter;
import com.url.shortner.security.jwt.JwtAuthenticationResponse;
import com.url.shortner.security.jwt.JwtUtils;
import com.url.shortner.service.RateLimiterService;
import com.url.shortner.service.TokenBlacklistService;
import com.url.shortner.service.UserService;
import com.url.shortner.util.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

    @MockBean
    private RateLimiterService rateLimiterService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private ClientIpResolver clientIpResolver;

    @Test
    void register_withValidPayload_returns200() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");

        mockMvc.perform(post("/api/auth/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered Successfully"));

        verify(userService).registerUser(any());
    }

    @Test
    void register_withDuplicateUsername_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        doThrow(new DuplicateResourceException("Username already exists")).when(userService).registerUser(any());

        mockMvc.perform(post("/api/auth/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void register_withBlankUsername_returns400WithFieldError() throws Exception {
        RegisterRequest request = new RegisterRequest("", "alice@example.com", "password123");

        mockMvc.perform(post("/api/auth/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void register_withInvalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "not-an-email", "password123");

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

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        LoginRequest request = new LoginRequest("alice", "wrongpassword");
        when(userService.authenticateUser(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    void login_whenRateLimited_returns429() throws Exception {
        LoginRequest request = new LoginRequest("alice", "password123");
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        doThrow(new RateLimitExceededException("Too many login attempts for this account. Please try again later."))
                .when(rateLimiterService).checkLoginLimit(anyString(), anyString());

        mockMvc.perform(post("/api/auth/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void logout_withValidToken_blacklistsItForItsRemainingLifetime() throws Exception {
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        when(jwtUtils.getJwtFromHeader(any())).thenReturn("some.jwt.token");
        when(jwtUtils.getExpirationFromJwtToken("some.jwt.token")).thenReturn(expiration);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer some.jwt.token"))
                .andExpect(status().isOk());

        verify(tokenBlacklistService).blacklist(eq("some.jwt.token"), any());
    }

    @Test
    void logout_withoutToken_doesNotBlacklistAnythingButStillReturns200() throws Exception {
        when(jwtUtils.getJwtFromHeader(any())).thenReturn(null);

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());

        verify(tokenBlacklistService, never()).blacklist(any(), any());
    }
}
