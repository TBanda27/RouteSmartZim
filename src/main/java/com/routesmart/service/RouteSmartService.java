package com.routesmart.service;

import com.routesmart.dto.RouteRequest;
import com.routesmart.dto.RouteResponse;
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

    public RouteSmartService(LocationParserService locationParserService,
                             GoogleMapsService googleMapsService) {
        this.locationParserService = locationParserService;
        this.googleMapsService = googleMapsService;
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

        // For now, return locations in original order (no optimization yet)
        // TODO: Add Python TSP optimization

        return RouteResponse.builder()
                .optimizedOrder(locations)
                .totalDistanceKm(0.0)
                .totalTimeMinutes(0)
                .isRoundTrip(request.getRouteType() == com.routesmart.enums.RouteType.ROUND_TRIP)
                .googleMapsUrl(buildGoogleMapsUrl(locations))
                .build();
    }

    private String buildGoogleMapsUrl(List<Location> locations) {
        if (locations.isEmpty()) return "";

        StringBuilder url = new StringBuilder("https://www.google.com/maps/dir/");
        for (Location loc : locations) {
            url.append(loc.getLatitude()).append(",").append(loc.getLongitude()).append("/");
        }
        return url.toString();
    }
}
