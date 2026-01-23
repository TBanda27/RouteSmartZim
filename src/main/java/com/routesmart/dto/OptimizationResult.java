package com.routesmart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.routesmart.model.Location;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OptimizationResult {

    @JsonProperty("optimized_order")
    private List<Integer> optimizedOrder;

    @JsonProperty("optimized_locations")
    private List<Location> optimizedLocations;

    @JsonProperty("total_distance_meters")
    private int totalDistanceMeters;

    @JsonProperty("total_distance_km")
    private double totalDistanceKm;

    @JsonProperty("route_description")
    private List<String> routeDescription;
}
