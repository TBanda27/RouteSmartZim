package com.routesmart.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "optimizer")
@Getter
@Setter
public class OptimizerConfig {

    private String pythonPath;
    private String scriptPath;
}
