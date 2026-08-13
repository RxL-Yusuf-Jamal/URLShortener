package com.url.shortner.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

// JWTs are verified statelessly (signature + expiry only), so logout on its own can't revoke one -
// the token stays valid until it expires no matter what the client does. This backs logout with a
// real server-side check: blacklisted tokens are stored in Redis with a TTL matching their own
// remaining lifetime, so entries clean themselves up and never outlive the token they block.
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private static final String KEY_PREFIX = "jwt:blacklisted:";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String token, Duration remainingValidity) {
        if (remainingValidity.isNegative() || remainingValidity.isZero()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + hash(token), "1", remainingValidity);
        } catch (Exception e) {
            // Best-effort, same as the redirect cache: losing this one logout's revocation to a
            // Redis blip is a far smaller problem than making logout itself fail because of it.
            log.warn("Failed to blacklist token on logout, Redis unavailable", e);
        }
    }

    // Fails open: this check backs an additional, defense-in-depth revocation layer on top of
    // normal JWT signature/expiry validation, not the primary auth mechanism. If Redis can't answer,
    // treating every request as unauthenticated would turn a Redis blip into an outage of the
    // entire authenticated surface of the app - a far worse outcome than a revoked token staying
    // valid for the remainder of its already-bounded lifetime until Redis recovers.
    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + hash(token)));
        } catch (Exception e) {
            log.warn("Failed to check token blacklist, Redis unavailable - allowing request through", e);
            return false;
        }
    }

    // Hash rather than store the raw token, so a compromised Redis instance doesn't hand out
    // still-valid bearer tokens.
    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
