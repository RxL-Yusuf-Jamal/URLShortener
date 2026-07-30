package com.url.shortner.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    // Render (like most reverse proxies/load balancers) puts the real client IP in X-Forwarded-For;
    // request.getRemoteAddr() alone would just return the proxy's IP, making per-IP limiting useless.
    public static String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
