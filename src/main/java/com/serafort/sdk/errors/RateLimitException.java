package com.serafort.sdk.errors;

public class RateLimitException extends SerafortException {
    private final Long retryAfterSeconds;

    public RateLimitException(String message, Long retryAfterSeconds) {
        super(message, "RATE_LIMIT_EXCEEDED", 429);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
