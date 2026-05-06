package com.ile.weather.service;

import dto.WeatherCurrentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Service
@Slf4j
public class WeatherStreamService {

    private final WeatherService weatherService;

    public WeatherStreamService(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    public Flux<WeatherCurrentDto> streamWeatherForLocation(String locationName) {
        return Flux.interval(Duration.ofSeconds(30))
                .flatMap(tick -> weatherService.getCurrentWeather(locationName))
                .onBackpressureDrop(dropped ->
                        log.warn("Dropped weather update due to backpressure for location: {}", locationName))
                .doOnCancel(() -> log.info("SSE client disconnected for location: {}", locationName))
                .doOnTerminate(() -> log.info("SSE stream terminated for location: {}", locationName));
    }

    public Flux<WeatherCurrentDto> streamWeatherForUser(String userId) {
        return weatherService.getSavedLocations(userId)
                .flatMap(location ->
                        Flux.interval(Duration.ofSeconds(30))
                                .flatMap(tick -> weatherService.getCurrentWeather(location.name()))
                )
                .onBackpressureDrop(dropped ->
                        log.warn("Dropped weather update due to backpressure for user: {}", userId))
                .doOnCancel(() -> log.info("SSE client disconnected for user: {}", userId))
                .doOnTerminate(() -> log.info("SSE stream terminated for user: {}", userId));
    }
}