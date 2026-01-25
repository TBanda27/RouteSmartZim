package com.routesmart.util;

import com.routesmart.enums.InputType;
import com.routesmart.model.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class LocationParserService {

    // Eircode pattern: D01 F5P2, D6W F4E2, A65F4E2, etc.
    // Format: 1 letter + 2 alphanumeric + optional space + 4 alphanumeric
    private static final Pattern EIRCODE_PATTERN = Pattern.compile(
            "^[A-Z][0-9A-Z]{2}[ ]?[A-Z0-9]{4}$",
            Pattern.CASE_INSENSITIVE
    );

    // Raw coordinates pattern: lat,lng (e.g., -17.8252,31.0335)
    private static final Pattern COORDINATES_PATTERN = Pattern.compile(
            "^(-?\\d+\\.\\d+),\\s*(-?\\d+\\.\\d+)$"
    );

    // Google Maps URL patterns for coordinate extraction
    private static final Pattern GOOGLE_MAPS_Q_PATTERN = Pattern.compile(
            "[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)"
    );
    private static final Pattern GOOGLE_MAPS_AT_PATTERN = Pattern.compile(
            "@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)"
    );
    private static final Pattern GOOGLE_MAPS_PLACE_PATTERN = Pattern.compile(
            "/place/(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)"
    );

    public List<Location> parseLocations(List<String> inputs) {
        List<Location> locations = new ArrayList<>();
        for (String input : inputs) {
            locations.add(parseLocation(input.trim()));
        }
        return locations;
    }

    public Location parseLocation(String input) {
        if (isCoordinates(input)) {
            return parseCoordinates(input);
        } else if (isGoogleMapsUrl(input)) {
            return parseGoogleMapsUrl(input);
        } else if (isEircode(input)) {
            return parseEircode(input);
        } else {
            return parseLocationName(input);
        }
    }

    private boolean isCoordinates(String input) {
        return COORDINATES_PATTERN.matcher(input.trim()).matches();
    }
    private Location parseCoordinates(String input) {
        Matcher matcher = COORDINATES_PATTERN.matcher(input.trim());
        if (matcher.matches()) {
            double latitude = Double.parseDouble(matcher.group(1));
            double longitude = Double.parseDouble(matcher.group(2));
            log.info("Parsed coordinates: {}, {}", latitude, longitude);
            return Location.builder()
                    .name("Current Location")
                    .originalInput(input)
                    .latitude(latitude)
                    .longitude(longitude)
                    .inputType(InputType.CURRENT_LOCATION)
                    .build();
        }
        return parseLocationName(input);
    }
    private boolean isGoogleMapsUrl(String input) {
        return input.contains("google.com/maps") || input.contains("goo.gl/maps");
    }

    private boolean isEircode(String input) {
        String cleaned = input.trim().replaceAll("\\s+", " ");
        boolean matches = EIRCODE_PATTERN.matcher(cleaned).matches();
        log.debug("Eircode check for '{}' (cleaned: '{}'): {}", input, cleaned, matches);
        return matches;
    }

    private Location parseGoogleMapsUrl(String input) {
        Double latitude = null;
        Double longitude = null;

        // Try different URL patterns
        Matcher matcher = GOOGLE_MAPS_Q_PATTERN.matcher(input);
        if (matcher.find()) {
            latitude = Double.parseDouble(matcher.group(1));
            longitude = Double.parseDouble(matcher.group(2));
        } else {
            matcher = GOOGLE_MAPS_AT_PATTERN.matcher(input);
            if (matcher.find()) {
                latitude = Double.parseDouble(matcher.group(1));
                longitude = Double.parseDouble(matcher.group(2));
            } else {
                matcher = GOOGLE_MAPS_PLACE_PATTERN.matcher(input);
                if (matcher.find()) {
                    latitude = Double.parseDouble(matcher.group(1));
                    longitude = Double.parseDouble(matcher.group(2));
                }
            }
        }

        if (latitude != null) {
            log.info("Extracted coordinates from URL: {}, {}", latitude, longitude);
            return Location.builder()
                    .name("Custom Location")
                    .originalInput(input)
                    .latitude(latitude)
                    .longitude(longitude)
                    .inputType(InputType.GOOGLE_MAPS_URL)
                    .build();
        }

        // URL but couldn't extract coords - treat as location name
        log.warn("Could not extract coordinates from Google Maps URL: {}", input);
        return Location.builder()
                .name(input)
                .originalInput(input)
                .inputType(InputType.LOCATION_NAME)
                .build();
    }

    private Location parseEircode(String input) {
        String eircode = input.toUpperCase().trim();
        log.info("Parsed Eircode: {}", eircode);
        return Location.builder()
                .name(eircode)
                .originalInput(input)
                .inputType(InputType.EIRCODE)
                .build();
    }

    private Location parseLocationName(String input) {
        String trimmed = input.trim();
        log.info("Parsed location name: {}", trimmed);
        return Location.builder()
                .name(trimmed)
                .originalInput(input)
                .inputType(InputType.LOCATION_NAME)
                .build();
    }
}
