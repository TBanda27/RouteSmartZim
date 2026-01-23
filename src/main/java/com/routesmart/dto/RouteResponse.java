package com.routesmart.dto;

import com.routesmart.model.Location;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class RouteResponse {

    private List<Location> optimizedOrder;
    private Double totalDistanceKm;
    private Integer totalTimeMinutes;
    private Boolean isRoundTrip;
    private String googleMapsUrl;
    private String embedMapUrl;
    private Integer remainingRequests;
    private List<String> routeDescription;
}
