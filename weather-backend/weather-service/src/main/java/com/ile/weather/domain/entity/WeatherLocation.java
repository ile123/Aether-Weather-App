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
@Table("weather_location")
public class WeatherLocation {

    @Id
    private UUID id;

    @Column("name")
    private String name;

    @Column("country")
    private String country;

    @Column("country_code")
    private String countryCode;

    @Column("timezone")
    private String timezone;

    @Column("population")
    private Long population;

    @Column("latitude")
    private BigDecimal latitude;

    @Column("longitude")
    private BigDecimal longitude;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;
}