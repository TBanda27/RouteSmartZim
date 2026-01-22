package com.routesmart.service;

import com.routesmart.config.RateLimitConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RateLimitService {

    private final Map<String, Bucket> buckets;
    private final RateLimitConfig rateLimitConfig;

    public RateLimitService(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
        this.buckets = new ConcurrentHashMap<>();
    }

    public boolean tryConsume(String clientIp) {
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);
        boolean consumed = bucket.tryConsume(1);
        log.debug("Rate limit check for IP {}: {}", clientIp, consumed ? "allowed" : "blocked");
        return consumed;
    }

    public int getRemainingRequests(String clientIp) {
        Bucket bucket = buckets.get(clientIp);
        if (bucket == null) {
            return rateLimitConfig.getRequestsPerHour();
        }
        return (int) bucket.getAvailableTokens();
    }

    private Bucket createBucket(String clientIp) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rateLimitConfig.getRequestsPerHour())
                .refillIntervally(rateLimitConfig.getRequestsPerHour(), Duration.ofHours(1))
                .build();
        log.info("Created new rate limit bucket for IP: {}", clientIp);
        return Bucket.builder().addLimit(limit).build();
    }
}
