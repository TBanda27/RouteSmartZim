package com.routesmart.model;

import com.routesmart.enums.InputType;
import com.routesmart.enums.RouteType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Location {

    private String name;
    private String originalInput;
    private Double latitude;
    private Double longitude;
    private InputType inputType;
    private Double distanceFromPrevious;
}
