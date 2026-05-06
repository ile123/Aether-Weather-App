package dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WeatherCurrentDto(
        String locationName,
        BigDecimal temperature,
        BigDecimal apparentTemperature,
        Integer relativeHumidity,
        BigDecimal windSpeed,
        Integer weatherCode,
        String description,
        Boolean isDay,
        Instant recordedAt
) {}