package com.url.shortner.controllers;

import com.url.shortner.exceptions.RateLimitExceededException;
import com.url.shortner.models.UrlMapping;
import com.url.shortner.security.jwt.JwtAuthenticationFilter;
import com.url.shortner.service.RateLimiterService;
import com.url.shortner.service.UrlMappingService;
import com.url.shortner.util.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// RedirectController's endpoint is public via a URL-pattern rule in the real WebSecurityConfig
// (not loaded in this slice), so the security filter chain is irrelevant here - same reasoning as AuthControllerTest.
@WebMvcTest(
        controllers = RedirectController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import(ClientIpResolver.class)
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlMappingService urlMappingService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    void redirect_whenFound_returns302WithLocation() throws Exception {
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl("https://example.com");
        when(urlMappingService.getOriginalUrl("abc123")).thenReturn(mapping);

        mockMvc.perform(get("/abc123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void redirect_whenNotFound_returns404() throws Exception {
        when(urlMappingService.getOriginalUrl("missing")).thenReturn(null);

        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirect_whenRateLimited_returns429() throws Exception {
        doThrow(new RateLimitExceededException("Too many requests. Please try again later."))
                .when(rateLimiterService).checkRedirectLimit(any());

        mockMvc.perform(get("/abc123"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void redirect_ignoresXForwardedForFromAnUntrustedPeerAndUsesRemoteAddr() throws Exception {
        // No app.trusted-proxy-ips configured in this slice, so 127.0.0.1 (MockHttpServletRequest's
        // default remote address) is not a trusted proxy - a self-reported X-Forwarded-For must be
        // ignored, or any caller could spoof a fresh IP per request and dodge rate limiting entirely.
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl("https://example.com");
        when(urlMappingService.getOriginalUrl("abc123")).thenReturn(mapping);

        mockMvc.perform(get("/abc123").header("X-Forwarded-For", "203.0.113.5, 10.0.0.1"))
                .andExpect(status().isFound());

        verify(rateLimiterService).checkRedirectLimit("127.0.0.1");
    }

    @Test
    void redirect_fallsBackToRemoteAddrWhenNoForwardedHeader() throws Exception {
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl("https://example.com");
        when(urlMappingService.getOriginalUrl("abc123")).thenReturn(mapping);

        mockMvc.perform(get("/abc123"))
                .andExpect(status().isFound());

        verify(rateLimiterService).checkRedirectLimit("127.0.0.1");
    }
}
