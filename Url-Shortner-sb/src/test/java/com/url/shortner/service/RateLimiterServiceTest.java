package com.url.shortner.service;

import com.url.shortner.exceptions.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterServiceTest {

    private final RateLimiterService rateLimiterService = new RateLimiterService();

    @Test
    void checkShortenLimit_allowsUpToFivePerMinuteThenThrows() {
        for (int i = 0; i < 5; i++) {
            assertThatCode(() -> rateLimiterService.checkShortenLimit("alice")).doesNotThrowAnyException();
        }

        assertThatThrownBy(() -> rateLimiterService.checkShortenLimit("alice"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void checkShortenLimit_isIndependentPerUser() {
        for (int i = 0; i < 5; i++) {
            rateLimiterService.checkShortenLimit("alice");
        }
        assertThatThrownBy(() -> rateLimiterService.checkShortenLimit("alice"))
                .isInstanceOf(RateLimitExceededException.class);

        assertThatCode(() -> rateLimiterService.checkShortenLimit("bob")).doesNotThrowAnyException();
    }

    @Test
    void checkRedirectLimit_allowsUpToTwentyPerMinuteThenThrows() {
        for (int i = 0; i < 20; i++) {
            assertThatCode(() -> rateLimiterService.checkRedirectLimit("203.0.113.5")).doesNotThrowAnyException();
        }

        assertThatThrownBy(() -> rateLimiterService.checkRedirectLimit("203.0.113.5"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void checkRedirectLimit_isIndependentPerIp() {
        for (int i = 0; i < 20; i++) {
            rateLimiterService.checkRedirectLimit("203.0.113.5");
        }
        assertThatThrownBy(() -> rateLimiterService.checkRedirectLimit("203.0.113.5"))
                .isInstanceOf(RateLimitExceededException.class);

        assertThatCode(() -> rateLimiterService.checkRedirectLimit("198.51.100.7")).doesNotThrowAnyException();
    }

    @Test
    void checkLoginLimit_allowsUpToFivePerUsernamePerMinuteThenThrows() {
        // Vary the IP each call so only the per-username bucket is under test.
        for (int i = 0; i < 5; i++) {
            String ip = "203.0.113." + i;
            assertThatCode(() -> rateLimiterService.checkLoginLimit(ip, "alice")).doesNotThrowAnyException();
        }

        assertThatThrownBy(() -> rateLimiterService.checkLoginLimit("203.0.113.99", "alice"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void checkLoginLimit_isIndependentPerUsername() {
        for (int i = 0; i < 5; i++) {
            rateLimiterService.checkLoginLimit("203.0.113." + i, "alice");
        }
        assertThatThrownBy(() -> rateLimiterService.checkLoginLimit("203.0.113.99", "alice"))
                .isInstanceOf(RateLimitExceededException.class);

        assertThatCode(() -> rateLimiterService.checkLoginLimit("203.0.113.100", "bob")).doesNotThrowAnyException();
    }

    @Test
    void checkLoginLimit_allowsUpToTenPerIpPerMinuteThenThrows() {
        // Vary the username each call so only the per-IP bucket is under test.
        for (int i = 0; i < 10; i++) {
            String username = "user" + i;
            assertThatCode(() -> rateLimiterService.checkLoginLimit("203.0.113.5", username)).doesNotThrowAnyException();
        }

        assertThatThrownBy(() -> rateLimiterService.checkLoginLimit("203.0.113.5", "onemore"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void checkLoginLimit_isIndependentPerIp() {
        for (int i = 0; i < 10; i++) {
            rateLimiterService.checkLoginLimit("203.0.113.5", "user" + i);
        }
        assertThatThrownBy(() -> rateLimiterService.checkLoginLimit("203.0.113.5", "onemore"))
                .isInstanceOf(RateLimitExceededException.class);

        assertThatCode(() -> rateLimiterService.checkLoginLimit("198.51.100.7", "carol")).doesNotThrowAnyException();
    }
}
