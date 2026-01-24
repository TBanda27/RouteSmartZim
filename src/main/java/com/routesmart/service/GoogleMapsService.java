package com.routesmart.service;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import com.routesmart.config.GoogleMapsConfig;
import com.routesmart.exception.GoogleMapsApiException;
import com.routesmart.model.Location;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GoogleMapsService {

    private final GoogleMapsConfig googleMapsConfig;
    private GeoApiContext geoApiContext;

    public GoogleMapsService(GoogleMapsConfig googleMapsConfig) {
        this.googleMapsConfig = googleMapsConfig;
    }

    @PostConstruct
    public void init() {
        this.geoApiContext = new GeoApiContext.Builder()
                .apiKey(googleMapsConfig.getApiKey())
                .build();
        log.info("Google Maps API context initialized");
    }

    @PreDestroy
    public void cleanup() {
        if (geoApiContext != null) {
            geoApiContext.shutdown();
            log.info("Google Maps API context shutdown");
        }
    }

    public void geocodeLocation(Location location) {
        if (location.getLatitude() != null && location.getLongitude() != null) {
            // Already has coordinates - do reverse geocoding to get address
            reverseGeocodeLocation(location);
            return;
        }

        try {
            GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, location.getName()).await();

            if (results != null && results.length > 0) {
                GeocodingResult result = results[0];
                location.setLatitude(result.geometry.location.lat);
                location.setLongitude(result.geometry.location.lng);
                location.setName(result.formattedAddress);
                log.info("Geocoded '{}' -> lat: {}, lng: {}",
                        result.formattedAddress,
                        result.geometry.location.lat,
                        result.geometry.location.lng);
            } else {
                log.error("Geocoding failed for '{}': No results found", location.getName());
            }
        } catch (Exception e) {
            log.error("Error geocoding location '{}': {}", location.getName(), e.getMessage());
        }
    }

    private void reverseGeocodeLocation(Location location) {
        try {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            GeocodingResult[] results = GeocodingApi.reverseGeocode(geoApiContext, latLng).await();

            if (results != null && results.length > 0) {
                String formattedAddress = results[0].formattedAddress;
                location.setName(formattedAddress);
                log.info("Reverse geocoded ({}, {}) -> {}",
                        location.getLatitude(),
                        location.getLongitude(),
                        formattedAddress);
            } else {
                log.warn("Reverse geocoding failed for ({}, {}): No results",
                        location.getLatitude(), location.getLongitude());
            }
        } catch (Exception e) {
            log.error("Error reverse geocoding ({}, {}): {}",
                    location.getLatitude(), location.getLongitude(), e.getMessage());
        }
    }

    public void geocodeLocations(List<Location> locations) {
        for (Location location : locations) {
            geocodeLocation(location);
        }
    }

    public com.routesmart.dto.DirectionsResult getOptimizedRoute(List<Location> locations, boolean isRoundTrip) {
        if (locations.size() < 2) {
            throw new GoogleMapsApiException("At least 2 locations are required for route optimization");
        }

        // Validate all locations have coordinates
        for (Location loc : locations) {
            if (loc.getLatitude() == null || loc.getLongitude() == null) {
                throw new GoogleMapsApiException("Location '" + loc.getName() + "' has no coordinates");
            }
        }

        try {
            Location origin = locations.get(0);
            Location destination = isRoundTrip ? origin : locations.get(locations.size() - 1);

            // Build waypoints (intermediate locations)
            List<String> waypoints = new ArrayList<>();
            int waypointEndIndex = isRoundTrip ? locations.size() : locations.size() - 1;
            for (int i = 1; i < waypointEndIndex; i++) {
                Location loc = locations.get(i);
                waypoints.add(loc.getLatitude() + "," + loc.getLongitude());
            }

            // Build the Directions API request
            DirectionsApi.RouteRestriction[] restrictions = {};
            DirectionsResult result;

            if (waypoints.isEmpty()) {
                // Direct route between origin and destination
                result = DirectionsApi.newRequest(geoApiContext)
                        .origin(new LatLng(origin.getLatitude(), origin.getLongitude()))
                        .destination(new LatLng(destination.getLatitude(), destination.getLongitude()))
                        .mode(TravelMode.DRIVING)
                        .await();
            } else {
                // Route with waypoints and optimization
                result = DirectionsApi.newRequest(geoApiContext)
                        .origin(new LatLng(origin.getLatitude(), origin.getLongitude()))
                        .destination(new LatLng(destination.getLatitude(), destination.getLongitude()))
                        .waypoints(waypoints.toArray(new String[0]))
                        .optimizeWaypoints(true)
                        .mode(TravelMode.DRIVING)
                        .await();
            }

            if (result.routes == null || result.routes.length == 0) {
                throw new GoogleMapsApiException("No route found between the specified locations");
            }

            DirectionsRoute route = result.routes[0];
            return parseDirectionsResult(locations, route, isRoundTrip);

        } catch (GoogleMapsApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting optimized route: {}", e.getMessage(), e);
            throw new GoogleMapsApiException("Failed to get optimized route: " + e.getMessage(), e);
        }
    }

    private com.routesmart.dto.DirectionsResult parseDirectionsResult(List<Location> locations,
                                                                       DirectionsRoute route,
                                                                       boolean isRoundTrip) {
        // Get the waypoint order from Google's optimization
        int[] waypointOrder = route.waypointOrder;

        // Build the optimized location list
        List<Location> optimizedLocations = new ArrayList<>();
        List<Integer> optimizedOrder = new ArrayList<>();
        List<String> routeDescription = new ArrayList<>();

        // Start with origin (index 0)
        optimizedLocations.add(locations.get(0));
        optimizedOrder.add(0);

        // Add waypoints in optimized order
        if (waypointOrder != null && waypointOrder.length > 0) {
            for (int wpIndex : waypointOrder) {
                // waypointOrder indices are 0-based relative to waypoints (not including origin)
                int locationIndex = wpIndex + 1;
                optimizedLocations.add(locations.get(locationIndex));
                optimizedOrder.add(locationIndex);
            }
        }

        // Add destination if not round trip (for round trip, destination is same as origin)
        if (!isRoundTrip && locations.size() > 1) {
            int destIndex = locations.size() - 1;
            optimizedLocations.add(locations.get(destIndex));
            optimizedOrder.add(destIndex);
        }

        // Calculate total distance and duration from legs
        long totalDistanceMeters = 0;
        long totalDurationSeconds = 0;

        for (int i = 0; i < route.legs.length; i++) {
            DirectionsLeg leg = route.legs[i];
            totalDistanceMeters += leg.distance.inMeters;
            totalDurationSeconds += leg.duration.inSeconds;

            // Set distance from previous for each location
            if (i < optimizedLocations.size()) {
                double legDistanceKm = leg.distance.inMeters / 1000.0;
                if (i + 1 < optimizedLocations.size()) {
                    optimizedLocations.get(i + 1).setDistanceFromPrevious(
                            Math.round(legDistanceKm * 100.0) / 100.0);
                }

                // Build route description
                String fromName = i == 0 ? locations.get(0).getName() : optimizedLocations.get(i).getName();
                String toName = i + 1 < optimizedLocations.size() ?
                        optimizedLocations.get(i + 1).getName() :
                        (isRoundTrip ? locations.get(0).getName() : "destination");
                routeDescription.add(String.format("%s -> %s: %.2f km",
                        fromName, toName, legDistanceKm));
            }
        }

        // Set first location distance to 0
        if (!optimizedLocations.isEmpty()) {
            optimizedLocations.get(0).setDistanceFromPrevious(0.0);
        }

        double totalDistanceKm = Math.round(totalDistanceMeters / 10.0) / 100.0;
        int totalDurationMinutes = (int) Math.ceil(totalDurationSeconds / 60.0);

        log.info("=== Optimized Route ===");
        log.info("Total distance: {} km", totalDistanceKm);
        log.info("Total duration: {} minutes", totalDurationMinutes);
        for (String step : routeDescription) {
            log.info(step);
        }

        return com.routesmart.dto.DirectionsResult.builder()
                .optimizedLocations(optimizedLocations)
                .waypointOrder(optimizedOrder)
                .totalDistanceKm(totalDistanceKm)
                .totalDurationMinutes(totalDurationMinutes)
                .routeDescription(routeDescription)
                .build();
    }
}
