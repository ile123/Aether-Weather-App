package dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WeatherUpdateEvent(String locationName, BigDecimal temperature, BigDecimal windSpeed,
                                 BigDecimal precipitation, Integer humidity, LocalDateTime recordedAt) {
}
