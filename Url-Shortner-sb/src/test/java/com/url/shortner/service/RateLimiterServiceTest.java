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
}
