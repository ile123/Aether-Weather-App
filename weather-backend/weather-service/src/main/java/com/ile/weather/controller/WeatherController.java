package com.ile.weather.controller;

import com.ile.weather.service.WeatherService;
import com.ile.weather.service.WeatherStreamService;
import dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/weather")
@Validated
public class WeatherController {

    private final WeatherService weatherService;
    private final WeatherStreamService weatherStreamService;

    public WeatherController(WeatherService weatherService, WeatherStreamService weatherStreamService) {
        this.weatherService = weatherService;
        this.weatherStreamService = weatherStreamService;
    }

    @GetMapping("/current")
    public Mono<ResponseEntity<ApiResponse<WeatherCurrentDto>>> getCurrentWeather(
            @RequestParam @NotBlank String location) {
        return weatherService.getCurrentWeather(location)
                .map(result -> ResponseEntity.ok(
                        ApiResponse.<WeatherCurrentDto>builder()
                                .success(true)
                                .data(result)
                                .timestamp(LocalDateTime.from(Instant.now()))
                                .build()
                ))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/forecast")
    public Flux<WeatherForecastDto> getForecast(
            @RequestParam @NotBlank String location,
            @RequestParam(defaultValue = "7") @Min(1) @Max(14) int days) {
        return weatherService.getForecast(location, days);
    }

    @GetMapping("/locations")
    public Flux<WeatherLocationDto> getSavedLocations(
            @AuthenticationPrincipal Jwt jwt) {
        return weatherService.getSavedLocations(jwt.getSubject());
    }

    @PostMapping("/locations")
    public Mono<ResponseEntity<ApiResponse<WeatherLocationDto>>> saveLocation(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid SaveLocationRequest request) {
        return weatherService.saveLocation(jwt.getSubject(), request.locationName())
                .map(result -> ResponseEntity.status(HttpStatus.CREATED).body(
                        ApiResponse.<WeatherLocationDto>builder()
                                .success(true)
                                .data(result)
                                .timestamp(LocalDateTime.from(Instant.now()))
                                .build()
                ));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<WeatherCurrentDto>> streamWeather(
            @RequestParam @NotBlank String location,
            @AuthenticationPrincipal Jwt jwt) {
        return weatherStreamService.streamWeatherForLocation(location)
                .map(weatherData -> ServerSentEvent.<WeatherCurrentDto>builder()
                        .id(UUID.randomUUID().toString())
                        .event("weather-update")
                        .data(weatherData)
                        .build()
                )
                .mergeWith(
                        Flux.interval(Duration.ofSeconds(15))
                                .map(tick -> ServerSentEvent.<WeatherCurrentDto>builder()
                                        .comment("keep-alive")
                                        .build()
                                )
                );
    }

    @GetMapping(value = "/stream/all", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<WeatherCurrentDto>> streamAllLocations(
            @AuthenticationPrincipal Jwt jwt) {
        return weatherStreamService.streamWeatherForUser(jwt.getSubject())
                .map(weatherData -> ServerSentEvent.<WeatherCurrentDto>builder()
                        .id(UUID.randomUUID().toString())
                        .event("weather-update")
                        .data(weatherData)
                        .build()
                )
                .mergeWith(
                        Flux.interval(Duration.ofSeconds(15))
                                .map(tick -> ServerSentEvent.<WeatherCurrentDto>builder()
                                        .comment("keep-alive")
                                        .build()
                                )
                );
    }
}