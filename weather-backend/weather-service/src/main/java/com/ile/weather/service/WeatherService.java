package com.ile.weather.service;

import aop.Audited;
import aop.LogExecutionTime;
import aop.ValidateLocation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ile.weather.client.OpenMeteoClient;
import com.ile.weather.domain.entity.WeatherCurrent;
import com.ile.weather.domain.entity.WeatherForecast;
import com.ile.weather.domain.entity.WeatherLocation;
import com.ile.weather.domain.repository.WeatherCurrentRepository;
import com.ile.weather.domain.repository.WeatherForecastRepository;
import com.ile.weather.domain.repository.WeatherLocationRepository;
import com.ile.weather.mapper.WeatherMapper;
import dto.WeatherCurrentDto;
import dto.WeatherForecastDto;
import dto.WeatherLocationDto;
import dto.WeatherUpdateEvent;
import exception.ExternalApiException;
import exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import mapper.WeatherDescriptionMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;

@Service
@Slf4j
public class WeatherService {

    private final OpenMeteoClient openMeteoClient;
    private final WeatherLocationRepository weatherLocationRepository;
    private final WeatherCurrentRepository weatherCurrentRepository;
    private final WeatherForecastRepository weatherForecastRepository;
    private final WeatherMapper weatherMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public WeatherService(
            OpenMeteoClient openMeteoClient,
            WeatherLocationRepository weatherLocationRepository,
            WeatherCurrentRepository weatherCurrentRepository,
            WeatherForecastRepository weatherForecastRepository,
            WeatherMapper weatherMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.openMeteoClient = openMeteoClient;
        this.weatherLocationRepository = weatherLocationRepository;
        this.weatherCurrentRepository = weatherCurrentRepository;
        this.weatherForecastRepository = weatherForecastRepository;
        this.weatherMapper = weatherMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @LogExecutionTime
    @ValidateLocation
    @Audited(action = "FETCH_WEATHER", logArgs = true)
    public Mono<WeatherCurrentDto> getCurrentWeather(String locationName) {
        return weatherLocationRepository.findByName(locationName)
                .switchIfEmpty(
                        openMeteoClient.geocodeLocation(locationName)
                                .flatMap(geocodingResponse -> {
                                    if (geocodingResponse.results().isEmpty()) {
                                        return Mono.<WeatherLocation>error(
                                                new ResourceNotFoundException("Location not found: ", locationName));
                                    }
                                    var result = geocodingResponse.results().getFirst();
                                    var newLocation = WeatherLocation.builder()
                                            .name(result.name())
                                            .country(result.country())
                                            .countryCode(result.countryCode())
                                            .timezone(result.timezone())
                                            .population(result.population())
                                            .latitude(result.latitude())
                                            .longitude(result.longitude())
                                            .build();
                                    return weatherLocationRepository.save(newLocation);
                                })
                )
                .flatMap(location ->
                        openMeteoClient.getCurrentWeather(location.getLatitude(), location.getLongitude())
                                .flatMap(response -> {
                                    var current = response.current();
                                    var weatherCurrent = WeatherCurrent.builder()
                                            .locationId(location.getId())
                                            .temperature(current.temperature())
                                            .apparentTemperature(current.apparentTemperature())
                                            .relativeHumidity(current.relativeHumidity())
                                            .precipitation(current.precipitation())
                                            .windSpeed(current.windSpeed())
                                            .windDirection(current.windDirection())
                                            .surfacePressure(current.surfacePressure())
                                            .cloudCover(current.cloudCover())
                                            .weatherCode(current.weatherCode())
                                            .isDay(current.isDay() == 1)
                                            .recordedAt(LocalDateTime.now())
                                            .build();
                                    return weatherCurrentRepository.save(weatherCurrent)
                                            .flatMap(saved -> {
                                                try {
                                                    var event = new WeatherUpdateEvent(
                                                            location.getName(),
                                                            saved.getTemperature(),
                                                            saved.getWindSpeed(),
                                                            saved.getPrecipitation(),
                                                            saved.getRelativeHumidity(),
                                                            saved.getRecordedAt()
                                                    );
                                                    kafkaTemplate.send("weather-updates", location.getName(),
                                                            objectMapper.writeValueAsString(event));
                                                } catch (Exception e) {
                                                    log.warn("Failed to publish weather update event: {}", e.getMessage());
                                                }
                                                return Mono.just(weatherMapper.toWeatherCurrentDto(saved, location));
                                            });
                                })
                )
                .onErrorMap(e -> new ExternalApiException("Failed to fetch weather: ", e.getMessage()));
    }

    @LogExecutionTime
    @ValidateLocation
    public Flux<WeatherForecastDto> getForecast(String locationName, int days) {
        return weatherLocationRepository.findByName(locationName)
                .switchIfEmpty(
                        openMeteoClient.geocodeLocation(locationName)
                                .flatMap(geocodingResponse -> {
                                    if (geocodingResponse.results().isEmpty()) {
                                        return Mono.<WeatherLocation>error(
                                                new ResourceNotFoundException("Location not found: ", locationName));
                                    }
                                    var result = geocodingResponse.results().getFirst();
                                    var newLocation = WeatherLocation.builder()
                                            .name(result.name())
                                            .country(result.country())
                                            .countryCode(result.countryCode())
                                            .timezone(result.timezone())
                                            .population(result.population())
                                            .latitude(result.latitude())
                                            .longitude(result.longitude())
                                            .build();
                                    return weatherLocationRepository.save(newLocation);
                                })
                )
                .flatMapMany(location ->
                        weatherForecastRepository.deleteAllByLocationId(location.getId())
                                .then(openMeteoClient.getForecast(location.getLatitude(), location.getLongitude(), days))
                                .flatMapMany(response -> {
                                    var hourly = response.hourly();
                                    var forecasts = new ArrayList<WeatherForecast>();

                                    for (int i = 0; i < hourly.time().size(); i++) {
                                        forecasts.add(WeatherForecast.builder()
                                                .locationId(location.getId())
                                                .forecastTime(LocalDateTime.parse(hourly.time().get(i)))
                                                .temperature(hourly.temperature().get(i))
                                                .apparentTemperature(hourly.apparentTemperature().get(i))
                                                .precipitationProbability(hourly.precipitationProbability().get(i))
                                                .precipitation(hourly.precipitation().get(i))
                                                .windSpeed(hourly.windSpeed().get(i))
                                                .windDirection(hourly.windDirection().get(i))
                                                .weatherCode(hourly.weatherCode().get(i))
                                                .cloudCover(hourly.cloudCover().get(i))
                                                .relativeHumidity(hourly.relativeHumidity().get(i))
                                                .isDay(hourly.isDay().get(i) == 1)
                                                .build());
                                    }

                                    return weatherForecastRepository.saveAll(forecasts)
                                            .map(saved -> new WeatherForecastDto(
                                                    location.getName(),
                                                    saved.getForecastTime().toInstant(ZoneOffset.UTC),
                                                    saved.getTemperature(),
                                                    saved.getPrecipitationProbability(),
                                                    saved.getPrecipitation(),
                                                    saved.getWeatherCode(),
                                                    WeatherDescriptionMapper.getDescription(saved.getWeatherCode())
                                            ));
                                })
                )
                .onErrorMap(e -> new ExternalApiException("Failed to fetch forecast: ", e.getMessage()));
    }

    public Flux<WeatherLocationDto> getSavedLocations(String userId) {
        return weatherLocationRepository.findByUserId(userId)
                .map(location -> new WeatherLocationDto(
                        location.getId(),
                        location.getName(),
                        location.getCountry(),
                        location.getLatitude(),
                        location.getLongitude()
                ));
    }

    public Mono<WeatherLocationDto> saveLocation(String userId, String locationName) {
        return openMeteoClient.geocodeLocation(locationName)
                .flatMap(geocodingResponse -> {
                    if (geocodingResponse.results().isEmpty()) {
                        return Mono.<WeatherLocation>error(
                                new ResourceNotFoundException("Location not found: ", locationName));
                    }
                    var result = geocodingResponse.results().getFirst();
                    var location = WeatherLocation.builder()
                            .userId(userId)
                            .name(result.name())
                            .country(result.country())
                            .countryCode(result.countryCode())
                            .timezone(result.timezone())
                            .population(result.population())
                            .latitude(result.latitude())
                            .longitude(result.longitude())
                            .build();
                    return weatherLocationRepository.save(location);
                })
                .map(saved -> new WeatherLocationDto(
                        saved.getId(),
                        saved.getName(),
                        saved.getCountry(),
                        saved.getLatitude(),
                        saved.getLongitude()
                ))
                .onErrorMap(e -> new ExternalApiException("Failed to save location: ", e.getMessage()));
    }
}