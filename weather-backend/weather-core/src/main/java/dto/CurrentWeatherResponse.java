package dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record CurrentWeatherResponse(

        @JsonProperty("latitude")
        BigDecimal latitude,

        @JsonProperty("longitude")
        BigDecimal longitude,

        @JsonProperty("timezone")
        String timezone,

        @JsonProperty("current")
        Current current
) {

    public record Current(

            @JsonProperty("time")
            String time,

            @JsonProperty("temperature_2m")
            BigDecimal temperature,

            @JsonProperty("apparent_temperature")
            BigDecimal apparentTemperature,

            @JsonProperty("relative_humidity_2m")
            Integer relativeHumidity,

            @JsonProperty("precipitation")
            BigDecimal precipitation,

            @JsonProperty("wind_speed_10m")
            BigDecimal windSpeed,

            @JsonProperty("wind_direction_10m")
            Integer windDirection,

            @JsonProperty("surface_pressure")
            BigDecimal surfacePressure,

            @JsonProperty("cloud_cover")
            Integer cloudCover,

            @JsonProperty("weather_code")
            Integer weatherCode,

            @JsonProperty("is_day")
            Integer isDay
    ) {
    }
}