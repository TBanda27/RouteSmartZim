package com.routesmart.service;

import com.routesmart.config.GoogleMapsConfig;
import com.routesmart.dto.OptimizationResult;
import com.routesmart.dto.RouteRequest;
import com.routesmart.dto.RouteResponse;
import com.routesmart.enums.RouteType;
import com.routesmart.model.Location;
import com.routesmart.util.LocationParserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class RouteSmartService {

    private final LocationParserService locationParserService;
    private final GoogleMapsService googleMapsService;
    private final OptimizerService optimizerService;
    private final GoogleMapsConfig googleMapsConfig;

    public RouteSmartService(LocationParserService locationParserService,
                             GoogleMapsService googleMapsService,
                             OptimizerService optimizerService,
                             GoogleMapsConfig googleMapsConfig) {
        this.locationParserService = locationParserService;
        this.googleMapsService = googleMapsService;
        this.optimizerService = optimizerService;
        this.googleMapsConfig = googleMapsConfig;
    }

    public RouteResponse optimizeRoute(RouteRequest request) {
        log.info("Starting route optimization for {} locations", request.getLocations().size());

        // Step 1: Parse input strings into Location objects
        List<Location> locations = locationParserService.parseLocations(request.getLocations());
        log.info("Parsed {} locations", locations.size());

        // Step 2: Geocode locations that don't have coordinates
        googleMapsService.geocodeLocations(locations);

        // Log all coordinates
        log.info("=== Location Coordinates ===");
        for (Location loc : locations) {
            log.info("{}: lat={}, lng={}, type={}",
                    loc.getName(),
                    loc.getLatitude(),
                    loc.getLongitude(),
                    loc.getInputType());
        }

        // Step 3: Get distance matrix
        int[][] distanceMatrix = googleMapsService.getDistanceMatrix(locations);

        // Step 4: Optimize route using Python service
        boolean isRoundTrip = request.getRouteType() == RouteType.ROUND_TRIP;
        OptimizationResult result = optimizerService.optimize(locations, distanceMatrix, isRoundTrip);

        log.info("=== Optimized Route ===");
        for (String step : result.getRouteDescription()) {
            log.info(step);
        }

        // Set distance from previous for each location
        List<Location> optimizedLocations = result.getOptimizedLocations();
        List<Integer> order = result.getOptimizedOrder();
        for (int i = 0; i < optimizedLocations.size(); i++) {
            if (i == 0) {
                optimizedLocations.get(i).setDistanceFromPrevious(0.0);
            } else {
                int prevIdx = order.get(i - 1);
                int currIdx = order.get(i);
                double distanceKm = distanceMatrix[prevIdx][currIdx] / 1000.0;
                optimizedLocations.get(i).setDistanceFromPrevious(Math.round(distanceKm * 100.0) / 100.0);
            }
        }

        return RouteResponse.builder()
                .optimizedOrder(optimizedLocations)
                .totalDistanceKm(result.getTotalDistanceKm())
                .totalTimeMinutes(0)
                .isRoundTrip(isRoundTrip)
                .googleMapsUrl(buildGoogleMapsUrl(optimizedLocations, isRoundTrip))
                .embedMapUrl(buildEmbedMapUrl(optimizedLocations, isRoundTrip))
                .routeDescription(result.getRouteDescription())
                .build();
    }

    private String buildGoogleMapsUrl(List<Location> locations, boolean isRoundTrip) {
        if (locations.isEmpty()) return "";

        StringBuilder url = new StringBuilder("https://www.google.com/maps/dir/");
        for (Location loc : locations) {
            url.append(loc.getLatitude()).append(",").append(loc.getLongitude()).append("/");
        }
        // For round trip, return to start
        if (isRoundTrip && !locations.isEmpty()) {
            Location start = locations.get(0);
            url.append(start.getLatitude()).append(",").append(start.getLongitude()).append("/");
        }
        return url.toString();
    }

    private String buildEmbedMapUrl(List<Location> locations, boolean isRoundTrip) {
        if (locations.size() < 2) return "";

        // Google Maps Embed API Directions URL
        StringBuilder url = new StringBuilder("https://www.google.com/maps/embed/v1/directions");
        url.append("?key=").append(googleMapsConfig.getApiKey());

        // Origin (first location)
        Location origin = locations.get(0);
        url.append("&origin=").append(origin.getLatitude()).append(",").append(origin.getLongitude());

        // Destination - for round trip, return to origin
        if (isRoundTrip) {
            url.append("&destination=").append(origin.getLatitude()).append(",").append(origin.getLongitude());
            // All other locations are waypoints
            if (locations.size() > 1) {
                url.append("&waypoints=");
                for (int i = 1; i < locations.size(); i++) {
                    if (i > 1) url.append("|");
                    Location loc = locations.get(i);
                    url.append(loc.getLatitude()).append(",").append(loc.getLongitude());
                }
            }
        } else {
            // One-way: destination is last location
            Location destination = locations.get(locations.size() - 1);
            url.append("&destination=").append(destination.getLatitude()).append(",").append(destination.getLongitude());
            // Middle locations are waypoints
            if (locations.size() > 2) {
                url.append("&waypoints=");
                for (int i = 1; i < locations.size() - 1; i++) {
                    if (i > 1) url.append("|");
                    Location loc = locations.get(i);
                    url.append(loc.getLatitude()).append(",").append(loc.getLongitude());
                }
            }
        }

        url.append("&mode=driving");

        return url.toString();
    }
}
