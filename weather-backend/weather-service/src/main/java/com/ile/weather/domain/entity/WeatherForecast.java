package com.ile.weather.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("weather_forecast")
public class WeatherForecast {

    @Id
    private UUID id;

    @Column("location_id")
    private UUID locationId;

    @Column("forecast_time")
    private LocalDateTime forecastTime;

    @Column("temperature")
    private BigDecimal temperature;

    @Column("apparent_temperature")
    private BigDecimal apparentTemperature;

    @Column("precipitation_probability")
    private Integer precipitationProbability;

    @Column("precipitation")
    private BigDecimal precipitation;

    @Column("wind_speed")
    private BigDecimal windSpeed;

    @Column("wind_direction")
    private Integer windDirection;

    @Column("weather_code")
    private Integer weatherCode;

    @Column("cloud_cover")
    private Integer cloudCover;

    @Column("relative_humidity")
    private Integer relativeHumidity;

    @Column("is_day")
    private Boolean isDay;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;
}