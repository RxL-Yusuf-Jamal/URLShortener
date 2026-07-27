package com.url.shortner.exceptions;

// Caller has exceeded an enforced rate limit (see RateLimiterService)
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
