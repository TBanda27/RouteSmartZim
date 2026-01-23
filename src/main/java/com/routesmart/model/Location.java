package com.routesmart.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.routesmart.enums.InputType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    private String name;

    @JsonProperty("original_input")
    private String originalInput;

    private Double latitude;
    private Double longitude;

    @JsonProperty("input_type")
    private InputType inputType;

    @JsonProperty("distance_from_previous")
    private Double distanceFromPrevious;
}
