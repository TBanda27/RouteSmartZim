package com.routesmart.service;

import com.routesmart.config.OptimizerConfig;
import com.routesmart.dto.OptimizationResult;
import com.routesmart.enums.InputType;
import com.routesmart.model.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // Enables Mockito annotations
class OptimizerServiceTest {

    @Mock  // Creates a mock RestTemplate
    private RestTemplate restTemplate;

    private OptimizerService optimizerService;
    private OptimizerConfig optimizerConfig;

    @BeforeEach
    void setUp() {
        // Create test config
        optimizerConfig = new OptimizerConfig();
        optimizerConfig.setUrl("http://localhost:8001");

        // Inject mock RestTemplate
        optimizerService = new OptimizerService(optimizerConfig, restTemplate);
    }

    // ==================== optimize() TESTS ====================

    @Test
    void shouldCallOptimizerEndpoint() {
        // GIVEN
        List<Location> locations = createTestLocations();
        int[][] distanceMatrix = {{0, 100}, {100, 0}};
        OptimizationResult mockResult = createMockResult();

        // Mock RestTemplate to return our mock result
        when(restTemplate.postForObject(
                eq("http://localhost:8001/optimize"),
                any(HttpEntity.class),
                eq(OptimizationResult.class)
        )).thenReturn(mockResult);

        // WHEN
        OptimizationResult result = optimizerService.optimize(locations, distanceMatrix, true);

        // THEN
        assertNotNull(result);
        assertEquals(10.5, result.getTotalDistanceKm());

        // Verify RestTemplate was called exactly once
        verify(restTemplate, times(1)).postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(OptimizationResult.class)
        );
    }

    @Test
    void shouldThrowExceptionOnApiFailure() {
        // GIVEN
        List<Location> locations = createTestLocations();
        int[][] distanceMatrix = {{0, 100}, {100, 0}};

        // Mock RestTemplate to throw exception
        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(OptimizationResult.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // WHEN / THEN
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            optimizerService.optimize(locations, distanceMatrix, true);
        });

        assertTrue(exception.getMessage().contains("Failed to optimize route"));
    }

    @Test
    void shouldBuildCorrectUrl() {
        // GIVEN
        List<Location> locations = createTestLocations();
        int[][] distanceMatrix = {{0, 100}, {100, 0}};
        OptimizationResult mockResult = createMockResult();

        when(restTemplate.postForObject(
                eq("http://localhost:8001/optimize"),  // Verify exact URL
                any(HttpEntity.class),
                eq(OptimizationResult.class)
        )).thenReturn(mockResult);

        // WHEN
        optimizerService.optimize(locations, distanceMatrix, true);

        // THEN - URL was correct (verified by the eq() matcher above)
        verify(restTemplate).postForObject(
                eq("http://localhost:8001/optimize"),
                any(HttpEntity.class),
                eq(OptimizationResult.class)
        );
    }

    // ==================== isHealthy() TESTS ====================

    @Test
    void shouldReturnTrueWhenHealthy() {
        // GIVEN - health endpoint returns successfully
        when(restTemplate.getForObject(
                eq("http://localhost:8001/health"),
                eq(String.class)
        )).thenReturn("{\"status\": \"healthy\"}");

        // WHEN
        boolean healthy = optimizerService.isHealthy();

        // THEN
        assertTrue(healthy);
    }

    @Test
    void shouldReturnFalseWhenUnhealthy() {
        // GIVEN - health endpoint throws exception
        when(restTemplate.getForObject(
                anyString(),
                eq(String.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // WHEN
        boolean healthy = optimizerService.isHealthy();

        // THEN
        assertFalse(healthy);
    }

    // ==================== HELPER METHODS ====================

    private List<Location> createTestLocations() {
        return Arrays.asList(
                Location.builder()
                        .name("Location A")
                        .latitude(-17.8252)
                        .longitude(31.0335)
                        .inputType(InputType.LOCATION_NAME)
                        .build(),
                Location.builder()
                        .name("Location B")
                        .latitude(-17.8300)
                        .longitude(31.0400)
                        .inputType(InputType.LOCATION_NAME)
                        .build()
        );
    }

    private OptimizationResult createMockResult() {
        OptimizationResult result = new OptimizationResult();
        result.setOptimizedOrder(Arrays.asList(0, 1));
        result.setTotalDistanceKm(10.5);
        result.setTotalDistanceMeters(10500);
        result.setRouteDescription(Arrays.asList("Start at A", "Go to B"));
        return result;
    }
}
