package dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WeatherForecastDto(
        String locationName,
        Instant forecastTime,
        BigDecimal temperature,
        Integer precipitationProbability,
        BigDecimal precipitation,
        Integer weatherCode,
        String description
) {
}