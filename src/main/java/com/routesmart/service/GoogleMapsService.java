package com.routesmart.service;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.DistanceMatrixRow;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import com.routesmart.config.GoogleMapsConfig;
import com.routesmart.model.Location;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
            log.info("Location already has coordinates: {}", location.getName());
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

    public void geocodeLocations(List<Location> locations) {
        for (Location location : locations) {
            geocodeLocation(location);
        }
    }

    public int[][] getDistanceMatrix(List<Location> locations) {
        int size = locations.size();
        int[][] matrix = new int[size][size];

        // Validate all locations have coordinates
        for (Location loc : locations) {
            if (loc.getLatitude() == null || loc.getLongitude() == null) {
                log.error("Cannot get distance matrix - location '{}' has no coordinates", loc.getName());
                return matrix;
            }
        }

        try {
            // Build LatLng array
            LatLng[] latLngs = new LatLng[size];
            for (int i = 0; i < size; i++) {
                Location loc = locations.get(i);
                latLngs[i] = new LatLng(loc.getLatitude(), loc.getLongitude());
            }

            // Call Distance Matrix API
            DistanceMatrix result = DistanceMatrixApi.newRequest(geoApiContext)
                    .origins(latLngs)
                    .destinations(latLngs)
                    .mode(TravelMode.DRIVING)
                    .await();

            // Parse results
            for (int i = 0; i < result.rows.length; i++) {
                DistanceMatrixRow row = result.rows[i];
                for (int j = 0; j < row.elements.length; j++) {
                    DistanceMatrixElement element = row.elements[j];
                    if (element.status.name().equals("OK")) {
                        matrix[i][j] = (int) element.distance.inMeters;
                    } else {
                        matrix[i][j] = Integer.MAX_VALUE;
                    }
                }
            }

            logDistanceMatrix(locations, matrix);

        } catch (Exception e) {
            log.error("Error getting distance matrix: {}", e.getMessage(), e);
        }

        return matrix;
    }

    private void logDistanceMatrix(List<Location> locations, int[][] matrix) {
        log.info("=== Distance Matrix (km) ===");
        for (int i = 0; i < locations.size(); i++) {
            for (int j = 0; j < locations.size(); j++) {
                if (i != j) {
                    double km = matrix[i][j] / 1000.0;
                    log.info("{} -> {}: {} km",
                            locations.get(i).getName(),
                            locations.get(j).getName(),
                            String.format("%.2f", km));
                }
            }
        }
    }
}
