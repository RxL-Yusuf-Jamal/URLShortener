package com.url.shortner.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenBlacklistService = new TokenBlacklistService(redisTemplate);
    }

    @Test
    void blacklist_storesTokenUnderAHashedKeyWithTheGivenTtl() {
        tokenBlacklistService.blacklist("some.jwt.token", Duration.ofMinutes(30));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), eq("1"), eq(Duration.ofMinutes(30)));

        String key = keyCaptor.getValue();
        assertThat(key).startsWith("jwt:blacklisted:");
        // The raw token must never appear in the key - only its hash.
        assertThat(key).doesNotContain("some.jwt.token");
    }

    @Test
    void blacklist_withAlreadyExpiredDuration_doesNothing() {
        tokenBlacklistService.blacklist("some.jwt.token", Duration.ZERO);
        tokenBlacklistService.blacklist("some.jwt.token", Duration.ofSeconds(-5));

        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void isBlacklisted_reflectsWhatWasBlacklisted() {
        when(redisTemplate.hasKey(any())).thenReturn(false);
        assertThat(tokenBlacklistService.isBlacklisted("some.jwt.token")).isFalse();

        when(redisTemplate.hasKey(any())).thenReturn(true);
        assertThat(tokenBlacklistService.isBlacklisted("some.jwt.token")).isTrue();
    }

    @Test
    void hash_isDeterministicAndDistinctPerToken() {
        tokenBlacklistService.blacklist("token-a", Duration.ofMinutes(1));
        tokenBlacklistService.blacklist("token-a", Duration.ofMinutes(1));
        tokenBlacklistService.blacklist("token-b", Duration.ofMinutes(1));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations, org.mockito.Mockito.times(3)).set(keyCaptor.capture(), eq("1"), any(Duration.class));

        var keys = keyCaptor.getAllValues();
        assertThat(keys.get(0)).isEqualTo(keys.get(1)); // same token -> same key
        assertThat(keys.get(0)).isNotEqualTo(keys.get(2)); // different token -> different key
    }
}
