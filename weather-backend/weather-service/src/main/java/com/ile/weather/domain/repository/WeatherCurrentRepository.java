package com.ile.weather.domain.repository;

import com.ile.weather.domain.entity.WeatherCurrent;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface WeatherCurrentRepository
        extends ReactiveCrudRepository<WeatherCurrent, UUID> {

    @Query("""
            SELECT * FROM weather_current
            WHERE location_id = :locationId
            ORDER BY recorded_at DESC
            LIMIT 1
            """)
    Mono<WeatherCurrent> findLatestByLocationId(UUID locationId);

    @Query("""
            SELECT * FROM weather_current
            WHERE location_id = :locationId
            AND recorded_at BETWEEN :from AND :to
            ORDER BY recorded_at DESC
            """)
    Flux<WeatherCurrent> findByLocationIdAndRecordedAtBetween(
            UUID locationId, LocalDateTime from, LocalDateTime to);

    @Query("""
            DELETE FROM weather_current
            WHERE recorded_at < :before
            """)
    Mono<Void> deleteOlderThan(LocalDateTime before);
}