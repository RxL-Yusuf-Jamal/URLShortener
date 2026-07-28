package com.url.shortner.controllers;

import com.url.shortner.models.UrlMapping;
import com.url.shortner.service.RateLimiterService;
import com.url.shortner.service.UrlMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "Redirect", description = "Public short-link redirection (rate-limited per IP)")
public class RedirectController {

    private final UrlMappingService urlMappingService;

    private final RateLimiterService rateLimiterService;

    @Operation(summary = "Redirect to the original URL for a short code")
    @GetMapping("/{shortUrl}")
    public ResponseEntity<Void> redirect(@PathVariable @NotBlank(message = "Short URL cannot be blank") String shortUrl,
                                          HttpServletRequest request) {
        rateLimiterService.checkRedirectLimit(clientIp(request));

        UrlMapping urlMapping = urlMappingService.getOriginalUrl(shortUrl);
        if (urlMapping != null) {
            HttpHeaders httpHeaders = new HttpHeaders();

            // Ensure no double slash
            String originalUrl = urlMapping.getOriginalUrl();

            httpHeaders.add("Location", originalUrl);
            return ResponseEntity.status(302).headers(httpHeaders).build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Render (like most reverse proxies/load balancers) puts the real client IP in X-Forwarded-For;
    // request.getRemoteAddr() alone would just return the proxy's IP, making per-IP limiting useless.
    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

}
