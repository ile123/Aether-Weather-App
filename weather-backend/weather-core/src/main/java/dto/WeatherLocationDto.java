package dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WeatherLocationDto(
        UUID id,
        String name,
        String country,
        BigDecimal latitude,
        BigDecimal longitude
) {
}