package dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record ForecastResponse(

        @JsonProperty("latitude")
        BigDecimal latitude,

        @JsonProperty("longitude")
        BigDecimal longitude,

        @JsonProperty("timezone")
        String timezone,

        @JsonProperty("hourly")
        Hourly hourly
) {

    public record Hourly(

            @JsonProperty("time")
            List<String> time,

            @JsonProperty("temperature_2m")
            List<BigDecimal> temperature,

            @JsonProperty("apparent_temperature")
            List<BigDecimal> apparentTemperature,

            @JsonProperty("precipitation_probability")
            List<Integer> precipitationProbability,

            @JsonProperty("precipitation")
            List<BigDecimal> precipitation,

            @JsonProperty("wind_speed_10m")
            List<BigDecimal> windSpeed,

            @JsonProperty("wind_direction_10m")
            List<Integer> windDirection,

            @JsonProperty("weather_code")
            List<Integer> weatherCode,

            @JsonProperty("cloud_cover")
            List<Integer> cloudCover,

            @JsonProperty("relative_humidity_2m")
            List<Integer> relativeHumidity,

            @JsonProperty("is_day")
            List<Integer> isDay
    ) {
    }
}