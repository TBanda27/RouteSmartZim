package com.routesmart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.routesmart.model.Location;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class OptimizationRequest {

    private List<Location> locations;

    @JsonProperty("distance_matrix")
    private int[][] distanceMatrix;

    @JsonProperty("is_round_trip")
    private boolean isRoundTrip;
}
