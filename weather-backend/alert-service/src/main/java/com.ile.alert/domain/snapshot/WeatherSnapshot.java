package com.ile.alert.domain.snapshot;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WeatherSnapshot {
    private String locationName;
    private BigDecimal temperature;
    private BigDecimal windSpeed;
    private BigDecimal precipitation;
    private Integer humidity;
}
