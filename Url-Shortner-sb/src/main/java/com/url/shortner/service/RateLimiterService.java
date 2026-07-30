package com.url.shortner.service;

import com.url.shortner.exceptions.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

// In-memory token buckets, keyed per user (shorten) or per IP (redirect). Per-instance only - if this
// app ever runs on more than one server, limits reset per instance rather than being shared; a
// bucket4j-redis-backed ProxyManager would be the distributed equivalent, reusing the existing Redis.
@Service
public class RateLimiterService {

    private static final int SHORTEN_LIMIT_PER_MINUTE = 5;
    private static final int REDIRECT_LIMIT_PER_MINUTE = 20;
    private static final int LOGIN_LIMIT_PER_IP_PER_MINUTE = 10;
    private static final int LOGIN_LIMIT_PER_USERNAME_PER_MINUTE = 5;

    private final ConcurrentHashMap<String, Bucket> shortenBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> redirectBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> loginIpBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> loginUsernameBuckets = new ConcurrentHashMap<>();

    public void checkShortenLimit(String username) {
        Bucket bucket = shortenBuckets.computeIfAbsent(username, key -> newBucket(SHORTEN_LIMIT_PER_MINUTE));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many short URLs created recently. Please try again later.");
        }
    }

    public void checkRedirectLimit(String clientIp) {
        Bucket bucket = redirectBuckets.computeIfAbsent(clientIp, key -> newBucket(REDIRECT_LIMIT_PER_MINUTE));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many requests. Please try again later.");
        }
    }

    // Two dimensions guard against two different attacks: the per-IP bucket stops one source from
    // brute-forcing many accounts, the per-username bucket stops one account being brute-forced
    // from many IPs (e.g. a botnet). Both are checked on every attempt, successful or not.
    public void checkLoginLimit(String clientIp, String username) {
        Bucket ipBucket = loginIpBuckets.computeIfAbsent(clientIp, key -> newBucket(LOGIN_LIMIT_PER_IP_PER_MINUTE));
        if (!ipBucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many login attempts from this location. Please try again later.");
        }

        Bucket usernameBucket = loginUsernameBuckets.computeIfAbsent(username, key -> newBucket(LOGIN_LIMIT_PER_USERNAME_PER_MINUTE));
        if (!usernameBucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many login attempts for this account. Please try again later.");
        }
    }

    private Bucket newBucket(int limitPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(limitPerMinute)
                .refillGreedy(limitPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
