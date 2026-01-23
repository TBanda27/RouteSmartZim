package com.routesmart.util;

import com.routesmart.enums.InputType;
import com.routesmart.model.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocationParserServiceTest {

    private LocationParserService locationParserService;

    @BeforeEach
    void setUp() {
        locationParserService = new LocationParserService();
    }

    // ==================== COORDINATE TESTS ====================

    @Test
    void shouldParseRawCoordinates() {
        // GIVEN
        String input = "-17.8252,31.0335";

        // WHEN
        Location result = locationParserService.parseLocation(input);

        // THEN
        assertEquals(InputType.CURRENT_LOCATION, result.getInputType());
        assertEquals(-17.8252, result.getLatitude());
        assertEquals(31.0335, result.getLongitude());
        assertEquals(input, result.getOriginalInput());
    }

    @Test
    void shouldParseCoordinatesWithSpace() {
        // GIVEN
        String input = "-17.8252, 31.0335";

        // WHEN
        Location result = locationParserService.parseLocation(input);

        // THEN
        assertEquals(InputType.CURRENT_LOCATION, result.getInputType());
        assertEquals(-17.8252, result.getLatitude());
        assertEquals(31.0335, result.getLongitude());
    }

    @Test
    void shouldParsePositiveCoordinates() {
        // GIVEN
        String input = "51.5074,0.1278";

        // WHEN
        Location result = locationParserService.parseLocation(input);

        // THEN
        assertEquals(InputType.CURRENT_LOCATION, result.getInputType());
        assertEquals(51.5074, result.getLatitude());
        assertEquals(0.1278, result.getLongitude());
    }

    // ==================== GOOGLE MAPS URL TESTS ====================

    @Test
    void shouldParseGoogleMapsUrlWithAtSymbol() {
        // GIVEN
        String input = "https://www.google.com/maps/@-17.8252,31.0335,15z";

        // WHEN
        Location result = locationParserService.parseLocation(input);

        // THEN
        assertEquals(InputType.GOOGLE_MAPS_URL, result.getInputType());
        assertEquals(-17.8252, result.getLatitude());
        assertEquals(31.0335, result.getLongitude());
    }

    @Test
    void shouldParseGoogleMapsUrlWithQueryParam() {
        // GIVEN
        String input = "https://www.google.com/maps?q=-17.8252,31.0335";

        // WHEN
        Location result = locationParserService.parseLocation(input);

        // THEN
        assertEquals(InputType.GOOGLE_MAPS_URL, result.getInputType());
        assertEquals(-17.8252, result.getLatitude());
        assertEquals(31.0335, result.getLongitude());
    }

    @Test
    void shouldParseGoogleMapsUrlWithPlace() {
        // GIVEN
        String input = "https://www.google.com/maps/place/-17.8252,31.0335";

        // WHEN
        Location result = locationParserService.parseLocation(input);

        // THEN
        assertEquals(InputType.GOOGLE_MAPS_URL, result.getInputType());
        assertEquals(-17.8252, result.getLatitude());
        assertEquals(31.0335, result.getLongitude());
    }

    @Test
    void shouldFallbackToLocationNameForInvalidGoogleMapsUrl() {
        // GIVEN - URL without coordinates
        String input = "https://www.google.com/maps/place/Harare";

        // WHEN
        Location result = locationParserService.parseLocation(input);

        // THEN - falls back to LOCATION_NAME
        assertEquals(InputType.LOCATION_NAME, result.getInputType());
        assertNull(result.getLatitude());
        assertNull(result.getLongitude());
    }

    // ==================== EIRCODE TESTS ====================

    @Test
    void shouldParseEircodeWithSpace() {
        // GIVEN
        String input = "D01 F5P2";

        // WHEN
        Location result = locationParserService.parseLocation(input);

        // THEN
        assertEquals(InputType.EIRCODE, result.getInputType());
        assertEquals("D01 F5P2", result.getName());
    }

    @Test
    void shouldParseEircodeWithoutSpace() {
        // GIVEN
        String input = "D01F5P2";

        // WHEN
        Location result = locationParserService.parseLocation(input);

        // THEN
        assertEquals(InputType.EIRCODE, result.getInputType());
    }

    @Test
    void shouldParseEircodeCaseInsensitive() {
        // GIVEN - lowercase input
        String input = "d01 f5p2";

        // WHEN
        Location result = locationParserService.parseLocation(input);

        // THEN
        assertEquals(InputType.EIRCODE, result.getInputType());
        assertEquals("D01 F5P2", result.getName()); // Should be uppercase
    }

    // ==================== LOCATION NAME TESTS ====================

    @Test
    void shouldParseLocationName() {
        // GIVEN
        String input = "Harare, Zimbabwe";

        // WHEN
        Location result = locationParserService.parseLocation(input);

        // THEN
        assertEquals(InputType.LOCATION_NAME, result.getInputType());
        assertEquals("Harare, Zimbabwe", result.getName());
        assertEquals(input, result.getOriginalInput());
        assertNull(result.getLatitude());
        assertNull(result.getLongitude());
    }

    @Test
    void shouldTrimWhitespace() {
        // GIVEN - input with extra whitespace
        String input = "  Harare, Zimbabwe  ";

        // WHEN
        Location result = locationParserService.parseLocation(input);

        // THEN
        assertEquals("Harare, Zimbabwe", result.getName());         // Name is trimmed
        assertEquals("  Harare, Zimbabwe  ", result.getOriginalInput()); // Original preserved
    }

    // ==================== BATCH PARSING TEST ====================

    @Test
    void shouldParseMultipleLocations() {
        // GIVEN
        List<String> inputs = List.of(
                "-17.8252,31.0335",
                "D01 F5P2",
                "Harare, Zimbabwe"
        );

        // WHEN
        List<Location> results = locationParserService.parseLocations(inputs);

        // THEN
        assertEquals(3, results.size());
        assertEquals(InputType.CURRENT_LOCATION, results.get(0).getInputType());
        assertEquals(InputType.EIRCODE, results.get(1).getInputType());
        assertEquals(InputType.LOCATION_NAME, results.get(2).getInputType());
    }
}
