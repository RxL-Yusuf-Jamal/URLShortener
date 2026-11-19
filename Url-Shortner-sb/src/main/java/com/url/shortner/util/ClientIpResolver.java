package com.url.shortner.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

// X-Forwarded-For is a plain client-supplied header - trusting it unconditionally lets any caller
// spoof a fresh IP on every request and bypass IP-keyed rate limiting entirely. It's only meaningful
// when it was actually set by a reverse proxy sitting in front of this app, so it's only honored
// when the request's immediate TCP peer (request.getRemoteAddr(), which the client cannot forge)
// is in the explicitly configured trusted-proxy allowlist. With no proxy configured, every request
// falls back to its real socket peer address, which is safe by default but means all traffic behind
// an unconfigured proxy shares one rate-limit bucket - set app.trusted-proxy-ips once the proxy's
// address is known to restore accurate per-client attribution.
@Component
public class ClientIpResolver {

    private final Set<String> trustedProxyIps;

    public ClientIpResolver(@Value("${app.trusted-proxy-ips:}") String trustedProxyIps) {
        this.trustedProxyIps = trustedProxyIps.isBlank()
                ? Collections.emptySet()
                : Arrays.stream(trustedProxyIps.split(",")).map(String::trim).collect(Collectors.toSet());
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxyIps.contains(remoteAddr)) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }
}
