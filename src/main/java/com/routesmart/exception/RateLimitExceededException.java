package com.routesmart.exception;

public class RateLimitExceededException extends RuntimeException {

    private final String clientIp;
    private final int retryAfterSeconds;

    public RateLimitExceededException(String clientIp) {
        super("Rate limit exceeded for IP: " + clientIp);
        this.clientIp = clientIp;
        this.retryAfterSeconds = 86400;
    }

    public RateLimitExceededException(String clientIp, int retryAfterSeconds) {
        super("Rate limit exceeded for IP: " + clientIp);
        this.clientIp = clientIp;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getClientIp() {
        return clientIp;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
