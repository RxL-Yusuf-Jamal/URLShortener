package com.url.shortner.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Minimal, Redis-serializable stand-in for UrlMapping - just enough to serve a redirect and
// attribute a click event without a DB read on a cache hit.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CachedUrlMapping {
    private Long id;
    private String originalUrl;
    private LocalDateTime expiresAt;
}