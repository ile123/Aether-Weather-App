package com.ile.weather.domain.repository;

import com.ile.weather.domain.entity.WeatherForecast;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface WeatherForecastRepository
        extends ReactiveCrudRepository<WeatherForecast, UUID> {

    @Query("""
        SELECT * FROM weather_forecast
        WHERE location_id = :locationId
        AND forecast_time >= :from
        ORDER BY forecast_time ASC
        """)
    Flux<WeatherForecast> findUpcomingByLocationId(
            UUID locationId, LocalDateTime from);

    @Query("""
        SELECT * FROM weather_forecast
        WHERE location_id = :locationId
        AND forecast_time BETWEEN :from AND :to
        ORDER BY forecast_time ASC
        """)
    Flux<WeatherForecast> findByLocationIdAndForecastTimeBetween(
            UUID locationId, LocalDateTime from, LocalDateTime to);

    @Query("""
        DELETE FROM weather_forecast
        WHERE location_id = :locationId
        AND forecast_time < :before
        """)
    Mono<Void> deleteStaleByLocationId(UUID locationId, LocalDateTime before);

    Mono<Void> deleteAllByLocationId(UUID locationId);
}