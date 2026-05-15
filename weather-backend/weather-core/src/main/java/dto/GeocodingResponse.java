package dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record GeocodingResponse(
        @JsonProperty("results")
        List<GeocodingResult> results
) {

    public record GeocodingResult(
            @JsonProperty("id")
            Long id,

            @JsonProperty("name")
            String name,

            @JsonProperty("latitude")
            BigDecimal latitude,

            @JsonProperty("longitude")
            BigDecimal longitude,

            @JsonProperty("country")
            String country,

            @JsonProperty("country_code")
            String countryCode,

            @JsonProperty("timezone")
            String timezone,

            @JsonProperty("population")
            Long population
    ) {
    }
}