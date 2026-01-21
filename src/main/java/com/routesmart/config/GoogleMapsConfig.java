package com.routesmart.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "google.maps")
@Getter
@Setter
public class GoogleMapsConfig {

    private String apiKey;
}
