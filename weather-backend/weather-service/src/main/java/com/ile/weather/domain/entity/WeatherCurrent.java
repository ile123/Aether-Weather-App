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
@Table("weather_current")
public class WeatherCurrent {

    @Id
    private UUID id;

    @Column("location_id")
    private UUID locationId;

    @Column("temperature")
    private BigDecimal temperature;

    @Column("apparent_temperature")
    private BigDecimal apparentTemperature;

    @Column("relative_humidity")
    private Integer relativeHumidity;

    @Column("precipitation")
    private BigDecimal precipitation;

    @Column("wind_speed")
    private BigDecimal windSpeed;

    @Column("wind_direction")
    private Integer windDirection;

    @Column("surface_pressure")
    private BigDecimal surfacePressure;

    @Column("cloud_cover")
    private Integer cloudCover;

    @Column("weather_code")
    private Integer weatherCode;

    @Column("is_day")
    private Boolean isDay;

    @Column("recorded_at")
    private LocalDateTime recordedAt;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;
}