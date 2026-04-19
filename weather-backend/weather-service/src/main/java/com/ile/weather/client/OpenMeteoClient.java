package com.ile.weather.client;

import dto.CurrentWeatherResponse;
import dto.ForecastResponse;
import dto.GeocodingResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@Slf4j
public class OpenMeteoClient {

    private static final String GEOCODING_CACHE_PREFIX = "geocoding:";
    private static final String CURRENT_CACHE_PREFIX = "weather:current:";
    private static final String FORECAST_CACHE_PREFIX = "weather:forecast:";
    private static final Duration GEOCODING_TTL = Duration.ofHours(24);
    private static final Duration CURRENT_TTL = Duration.ofMinutes(5);
    private static final Duration FORECAST_TTL = Duration.ofMinutes(30);

    private final WebClient geocodingClient;
    private final WebClient forecastClient;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    public OpenMeteoClient(
            @Qualifier("geocodingWebClient") WebClient geocodingClient,
            @Qualifier("forecastWebClient") WebClient forecastClient,
            ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.geocodingClient = geocodingClient;
        this.forecastClient = forecastClient;
        this.redisTemplate = redisTemplate;
    }

    @CircuitBreaker(name = "openMeteo", fallbackMethod = "geocodingFallback")
    public Mono<GeocodingResponse> geocodeLocation(String locationName) {
        String cacheKey = GEOCODING_CACHE_PREFIX + locationName.toLowerCase();

        return redisTemplate.opsForValue().get(cacheKey)
                .cast(GeocodingResponse.class)
                .doOnNext(cached -> log.debug("[Cache HIT] geocoding: {}", locationName))
                .switchIfEmpty(fetchGeocoding(locationName)
                        .flatMap(response -> redisTemplate.opsForValue()
                                .set(cacheKey, response, GEOCODING_TTL)
                                .thenReturn(response)));
    }

    private Mono<GeocodingResponse> fetchGeocoding(String locationName) {
        log.info("[Open-Meteo] Geocoding: {}", locationName);
        return geocodingClient.get()
                .uri(uri -> uri
                        .path("/search")
                        .queryParam("name", locationName)
                        .queryParam("count", 5)
                        .queryParam("language", "en")
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(GeocodingResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryable)
                        .doBeforeRetry(signal ->
                                log.warn("[Open-Meteo] Retrying geocoding attempt {}",
                                        signal.totalRetries() + 1)))
                .timeout(Duration.ofSeconds(10));
    }

    @CircuitBreaker(name = "openMeteo", fallbackMethod = "currentWeatherFallback")
    public Mono<CurrentWeatherResponse> getCurrentWeather(
            BigDecimal latitude, BigDecimal longitude) {
        String cacheKey = CURRENT_CACHE_PREFIX + latitude + ":" + longitude;

        return redisTemplate.opsForValue().get(cacheKey)
                .cast(CurrentWeatherResponse.class)
                .doOnNext(cached -> log.debug("[Cache HIT] current weather: {}:{}", latitude, longitude))
                .switchIfEmpty(fetchCurrentWeather(latitude, longitude)
                        .flatMap(response -> redisTemplate.opsForValue()
                                .set(cacheKey, response, CURRENT_TTL)
                                .thenReturn(response)));
    }

    private Mono<CurrentWeatherResponse> fetchCurrentWeather(
            BigDecimal latitude, BigDecimal longitude) {
        log.info("[Open-Meteo] Fetching current weather for {}:{}", latitude, longitude);
        return forecastClient.get()
                .uri(uri -> uri
                        .path("/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("current",
                                "temperature_2m,apparent_temperature," +
                                        "relative_humidity_2m,precipitation," +
                                        "wind_speed_10m,wind_direction_10m," +
                                        "surface_pressure,cloud_cover,weather_code,is_day")
                        .queryParam("timezone", "auto")
                        .build())
                .retrieve()
                .bodyToMono(CurrentWeatherResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryable)
                        .doBeforeRetry(signal ->
                                log.warn("[Open-Meteo] Retrying current weather attempt {}",
                                        signal.totalRetries() + 1)))
                .timeout(Duration.ofSeconds(10));
    }

    @CircuitBreaker(name = "openMeteo", fallbackMethod = "forecastFallback")
    public Mono<ForecastResponse> getForecast(
            BigDecimal latitude, BigDecimal longitude, int days) {
        String cacheKey = FORECAST_CACHE_PREFIX + latitude + ":" + longitude + ":" + days;

        return redisTemplate.opsForValue().get(cacheKey)
                .cast(ForecastResponse.class)
                .doOnNext(cached -> log.debug("[Cache HIT] forecast: {}:{}", latitude, longitude))
                .switchIfEmpty(fetchForecast(latitude, longitude, days)
                        .flatMap(response -> redisTemplate.opsForValue()
                                .set(cacheKey, response, FORECAST_TTL)
                                .thenReturn(response)));
    }

    private Mono<ForecastResponse> fetchForecast(
            BigDecimal latitude, BigDecimal longitude, int days) {
        log.info("[Open-Meteo] Fetching {}d forecast for {}:{}", days, latitude, longitude);
        return forecastClient.get()
                .uri(uri -> uri
                        .path("/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("hourly",
                                "temperature_2m,apparent_temperature," +
                                        "precipitation_probability,precipitation," +
                                        "wind_speed_10m,wind_direction_10m," +
                                        "weather_code,cloud_cover," +
                                        "relative_humidity_2m,is_day")
                        .queryParam("forecast_days", days)
                        .queryParam("timezone", "auto")
                        .build())
                .retrieve()
                .bodyToMono(ForecastResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryable)
                        .doBeforeRetry(signal ->
                                log.warn("[Open-Meteo] Retrying forecast attempt {}",
                                        signal.totalRetries() + 1)))
                .timeout(Duration.ofSeconds(10));
    }

    private Mono<GeocodingResponse> geocodingFallback(
            String locationName, Throwable t) {
        log.error("[Circuit Breaker] Geocoding fallback for {}: {}", locationName, t.getMessage());
        return Mono.error(new RuntimeException(
                "Geocoding service unavailable for: " + locationName));
    }

    private Mono<CurrentWeatherResponse> currentWeatherFallback(
            BigDecimal lat, BigDecimal lon, Throwable t) {
        log.error("[Circuit Breaker] Current weather fallback for {}:{}: {}", lat, lon, t.getMessage());
        return Mono.error(new RuntimeException(
                "Weather service temporarily unavailable"));
    }

    private Mono<ForecastResponse> forecastFallback(
            BigDecimal lat, BigDecimal lon, int days, Throwable t) {
        log.error("[Circuit Breaker] Forecast fallback for {}:{}: {}", lat, lon, t.getMessage());
        return Mono.error(new RuntimeException(
                "Forecast service temporarily unavailable"));
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            return ex.getStatusCode().is5xxServerError();
        }
        return true;
    }
}