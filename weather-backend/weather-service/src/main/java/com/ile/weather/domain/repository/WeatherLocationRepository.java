package com.ile.weather.domain.repository;

import com.ile.weather.domain.entity.WeatherLocation;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface WeatherLocationRepository
        extends ReactiveCrudRepository<WeatherLocation, UUID> {

    Mono<WeatherLocation> findByName(String name);

    Mono<WeatherLocation> findByNameAndCountryCode(String name, String countryCode);

    Flux<WeatherLocation> findByCountryCode(String countryCode);

    @Query("""
            SELECT * FROM weather_location
            WHERE name ILIKE '%' || :query || '%'
            ORDER BY name
            LIMIT :limit
            """)
    Flux<WeatherLocation> searchByName(String query, int limit);

    @Query("""
            SELECT * FROM weather_location
            WHERE ABS(latitude - :lat) < :radius
            AND ABS(longitude - :lon) < :radius
            ORDER BY ABS(latitude - :lat) + ABS(longitude - :lon)
            LIMIT 10
            """)
    Flux<WeatherLocation> findNearbyLocations(
            BigDecimal lat, BigDecimal lon, BigDecimal radius);

    Flux<WeatherLocation> findByUserId(String userId);
}