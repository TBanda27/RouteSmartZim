package com.routesmart.dto;

import com.routesmart.enums.RouteType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RouteRequest {

    @NotEmpty(message = "Locations list cannot be empty")
    @Size(min = 2, max = 10, message = "Must have between 2 and 10 locations")
    private List<String> locations;

    @NotNull(message = "Route type is required")
    private RouteType routeType;
}
