package com.url.shortner.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private static final String PROXY_IP = "10.0.0.1";

    private static MockHttpServletRequest requestFrom(String remoteAddr, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    @Test
    void resolve_usesFirstXForwardedForAddressWhenPeerIsTrustedProxy() {
        ClientIpResolver resolver = new ClientIpResolver(PROXY_IP);

        String ip = resolver.resolve(requestFrom(PROXY_IP, "203.0.113.5, 10.0.0.1"));

        assertThat(ip).isEqualTo("203.0.113.5");
    }

    @Test
    void resolve_fallsBackToRemoteAddrWhenTrustedProxySendsNoXForwardedFor() {
        ClientIpResolver resolver = new ClientIpResolver(PROXY_IP);

        assertThat(resolver.resolve(requestFrom(PROXY_IP, null))).isEqualTo(PROXY_IP);
        assertThat(resolver.resolve(requestFrom(PROXY_IP, "   "))).isEqualTo(PROXY_IP);
    }

    @Test
    void resolve_ignoresXForwardedForWhenPeerIsNotTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(PROXY_IP);

        String ip = resolver.resolve(requestFrom("198.51.100.7", "203.0.113.5"));

        assertThat(ip).isEqualTo("198.51.100.7");
    }

    @Test
    void resolve_acceptsAnyEntryInCommaSeparatedTrustedListWithWhitespace() {
        ClientIpResolver resolver = new ClientIpResolver(" 10.0.0.1 , 10.0.0.2 ");

        assertThat(resolver.resolve(requestFrom("10.0.0.2", "203.0.113.9"))).isEqualTo("203.0.113.9");
        assertThat(resolver.resolve(requestFrom("10.0.0.1", "203.0.113.8"))).isEqualTo("203.0.113.8");
    }
}
