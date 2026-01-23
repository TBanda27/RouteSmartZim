package com.routesmart.service;

import com.routesmart.config.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;
    private RateLimitConfig rateLimitConfig;

    // Using a small limit (3) for easier testing
    private static final int TEST_REQUESTS_PER_DAY = 3;

    @BeforeEach
    void setUp() {
        // Create a test config with 3 requests per day
        rateLimitConfig = new RateLimitConfig();
        rateLimitConfig.setRequestsPerDay(TEST_REQUESTS_PER_DAY);

        // Create the service with our test config
        rateLimitService = new RateLimitService(rateLimitConfig);
    }

    // ==================== tryConsume TESTS ====================

    @Test
    void shouldAllowFirstRequest() {
        // GIVEN - a new IP address
        String clientIp = "192.168.1.1";

        // WHEN - first request
        boolean result = rateLimitService.tryConsume(clientIp);

        // THEN - should be allowed
        assertTrue(result);
    }

    @Test
    void shouldAllowRequestsUpToLimit() {
        // GIVEN
        String clientIp = "192.168.1.1";

        // WHEN - consume all 3 requests
        boolean first = rateLimitService.tryConsume(clientIp);
        boolean second = rateLimitService.tryConsume(clientIp);
        boolean third = rateLimitService.tryConsume(clientIp);

        // THEN - all should be allowed
        assertTrue(first);
        assertTrue(second);
        assertTrue(third);
    }

    @Test
    void shouldBlockAfterLimitExceeded() {
        // GIVEN
        String clientIp = "192.168.1.1";

        // WHEN - consume all 3 requests
        rateLimitService.tryConsume(clientIp);
        rateLimitService.tryConsume(clientIp);
        rateLimitService.tryConsume(clientIp);

        // THEN - 4th request should be blocked
        boolean fourthRequest = rateLimitService.tryConsume(clientIp);
        assertFalse(fourthRequest);
    }

    @Test
    void shouldTrackDifferentIpsSeparately() {
        // GIVEN - two different IPs
        String ip1 = "192.168.1.1";
        String ip2 = "192.168.1.2";

        // WHEN - exhaust limit for IP1
        rateLimitService.tryConsume(ip1);
        rateLimitService.tryConsume(ip1);
        rateLimitService.tryConsume(ip1);

        // THEN - IP1 is blocked, but IP2 is still allowed
        assertFalse(rateLimitService.tryConsume(ip1));
        assertTrue(rateLimitService.tryConsume(ip2));
    }

    // ==================== getRemainingRequests TESTS ====================

    @Test
    void shouldReturnMaxForNewIp() {
        // GIVEN - a new IP that hasn't made any requests
        String clientIp = "192.168.1.100";

        // WHEN
        int remaining = rateLimitService.getRemainingRequests(clientIp);

        // THEN - should return max requests
        assertEquals(TEST_REQUESTS_PER_DAY, remaining);
    }

    @Test
    void shouldDecreaseRemainingAfterConsume() {
        // GIVEN
        String clientIp = "192.168.1.1";

        // WHEN - consume 1 request
        rateLimitService.tryConsume(clientIp);

        // THEN - remaining should be max - 1
        int remaining = rateLimitService.getRemainingRequests(clientIp);
        assertEquals(TEST_REQUESTS_PER_DAY - 1, remaining);
    }

    @Test
    void shouldReturnZeroWhenExhausted() {
        // GIVEN
        String clientIp = "192.168.1.1";

        // WHEN - exhaust all requests
        for (int i = 0; i < TEST_REQUESTS_PER_DAY; i++) {
            rateLimitService.tryConsume(clientIp);
        }

        // THEN
        int remaining = rateLimitService.getRemainingRequests(clientIp);
        assertEquals(0, remaining);
    }

    @Test
    void shouldNotGoBelowZero() {
        // GIVEN
        String clientIp = "192.168.1.1";

        // WHEN - try to consume more than limit
        for (int i = 0; i < TEST_REQUESTS_PER_DAY + 5; i++) {
            rateLimitService.tryConsume(clientIp);
        }

        // THEN - remaining should be 0, not negative
        int remaining = rateLimitService.getRemainingRequests(clientIp);
        assertEquals(0, remaining);
    }
}
