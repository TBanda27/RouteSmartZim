package com.routesmart.dto;

import com.routesmart.model.Location;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class DirectionsResult {

    private List<Location> optimizedLocations;
    private List<Integer> waypointOrder;
    private Double totalDistanceKm;
    private Integer totalDurationMinutes;
    private List<String> routeDescription;
}
