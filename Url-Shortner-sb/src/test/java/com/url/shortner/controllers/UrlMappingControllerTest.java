package com.url.shortner.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.shortner.config.MethodSecurityTestConfig;
import com.url.shortner.dtos.UrlMappingDTO;
import com.url.shortner.dtos.UrlRequest;
import com.url.shortner.exceptions.RateLimitExceededException;
import com.url.shortner.models.User;
import com.url.shortner.security.jwt.JwtAuthenticationFilter;
import com.url.shortner.service.UrlMappingService;
import com.url.shortner.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UrlMappingController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import(MethodSecurityTestConfig.class)
class UrlMappingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlMappingService urlMappingService;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void createShortUrl_withValidUrl_returns200() throws Exception {
        User user = new User();
        user.setUsername("alice");
        when(userService.findByUsername("alice")).thenReturn(user);

        UrlMappingDTO dto = new UrlMappingDTO(1L, "https://example.com", "abc12345", 0, LocalDateTime.now(), null, "alice");
        when(urlMappingService.createShortUrl(eq("https://example.com"), any(), any(User.class))).thenReturn(dto);

        UrlRequest request = new UrlRequest("https://example.com", null);

        mockMvc.perform(post("/api/urls/shorten")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value("abc12345"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void createShortUrl_withExpiresInDays_passesThroughToService() throws Exception {
        User user = new User();
        user.setUsername("alice");
        when(userService.findByUsername("alice")).thenReturn(user);

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        UrlMappingDTO dto = new UrlMappingDTO(1L, "https://example.com", "abc12345", 0, LocalDateTime.now(), expiresAt, "alice");
        when(urlMappingService.createShortUrl(eq("https://example.com"), eq(7), any(User.class))).thenReturn(dto);

        UrlRequest request = new UrlRequest("https://example.com", 7);

        mockMvc.perform(post("/api/urls/shorten")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void createShortUrl_withInvalidUrl_returns400() throws Exception {
        UrlRequest request = new UrlRequest("not a url", null);

        mockMvc.perform(post("/api/urls/shorten")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void createShortUrl_withBlankUrl_returns400() throws Exception {
        UrlRequest request = new UrlRequest("", null);

        mockMvc.perform(post("/api/urls/shorten")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void createShortUrl_whenRateLimited_returns429() throws Exception {
        User user = new User();
        user.setUsername("alice");
        when(userService.findByUsername("alice")).thenReturn(user);
        when(urlMappingService.createShortUrl(any(), any(), any(User.class)))
                .thenThrow(new RateLimitExceededException("Too many short URLs created recently. Please try again later."));

        UrlRequest request = new UrlRequest("https://example.com", null);

        mockMvc.perform(post("/api/urls/shorten")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void createShortUrl_withoutAuthentication_isRejected() throws Exception {
        UrlRequest request = new UrlRequest("https://example.com", null);

        mockMvc.perform(post("/api/urls/shorten")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void deleteShortUrl_whenNotOwner_returns403() throws Exception {
        User user = new User();
        user.setUsername("alice");
        when(userService.findByUsername("alice")).thenReturn(user);
        doThrow(new SecurityException("You don't have permission to delete this URL"))
                .when(urlMappingService).deleteUrlMapping(eq("abc12345"), any(User.class));

        mockMvc.perform(delete("/api/urls/abc12345").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void deleteShortUrl_whenNotFound_returns404() throws Exception {
        User user = new User();
        user.setUsername("alice");
        when(userService.findByUsername("alice")).thenReturn(user);
        doThrow(new NoSuchElementException("URL mapping not found"))
                .when(urlMappingService).deleteUrlMapping(eq("missing"), any(User.class));

        mockMvc.perform(delete("/api/urls/missing").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
