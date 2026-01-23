package com.routesmart.service;

import com.routesmart.config.OptimizerConfig;
import com.routesmart.dto.OptimizationRequest;
import com.routesmart.dto.OptimizationResult;
import com.routesmart.model.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class OptimizerService {

    private final OptimizerConfig optimizerConfig;
    private final RestTemplate restTemplate;

    // Main constructor for production
    public OptimizerService(OptimizerConfig optimizerConfig) {
        this(optimizerConfig, new RestTemplate());
    }

    // Constructor for testing (allows injecting mock RestTemplate)
    public OptimizerService(OptimizerConfig optimizerConfig, RestTemplate restTemplate) {
        this.optimizerConfig = optimizerConfig;
        this.restTemplate = restTemplate;
    }

    public OptimizationResult optimize(List<Location> locations, int[][] distanceMatrix, boolean isRoundTrip) {
        log.info("Calling optimizer service for {} locations", locations.size());

        String url = optimizerConfig.getUrl() + "/optimize";

        OptimizationRequest request = OptimizationRequest.builder()
                .locations(locations)
                .distanceMatrix(distanceMatrix)
                .isRoundTrip(isRoundTrip)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<OptimizationRequest> entity = new HttpEntity<>(request, headers);

        try {
            OptimizationResult result = restTemplate.postForObject(url, entity, OptimizationResult.class);
            log.info("Optimization complete: {} km total distance", result.getTotalDistanceKm());
            return result;
        } catch (Exception e) {
            log.error("Error calling optimizer service: {}", e.getMessage());
            throw new RuntimeException("Failed to optimize route: " + e.getMessage(), e);
        }
    }

    public boolean isHealthy() {
        try {
            String url = optimizerConfig.getUrl() + "/health";
            restTemplate.getForObject(url, String.class);
            return true;
        } catch (Exception e) {
            log.warn("Optimizer service health check failed: {}", e.getMessage());
            return false;
        }
    }
}
