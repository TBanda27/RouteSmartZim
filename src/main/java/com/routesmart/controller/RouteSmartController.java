package com.routesmart.controller;

import com.routesmart.model.RouteRequest;
import com.routesmart.model.RouteResponse;
import com.routesmart.service.RateLimitService;
import com.routesmart.service.RouteSmartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class RouteSmartController {

    private final RouteSmartService routeSmartService;
    private final RateLimitService rateLimitService;

    public RouteSmartController(RouteSmartService routeSmartService,
                                RateLimitService rateLimitService) {
        this.routeSmartService = routeSmartService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/optimize")
    public ResponseEntity<RouteResponse> optimizeRoute(
            @Valid @RequestBody RouteRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);
        log.info("Received optimization request from IP: {} with {} locations",
                clientIp, request.getLocations().size());

        // Check rate limit
        if (!rateLimitService.tryConsume(clientIp)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        RouteResponse response = routeSmartService.optimizeRoute(request);
        response.setRemainingRequests(rateLimitService.getRemainingRequests(clientIp));

        log.info("Successfully optimized route for IP: {}", clientIp);
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
