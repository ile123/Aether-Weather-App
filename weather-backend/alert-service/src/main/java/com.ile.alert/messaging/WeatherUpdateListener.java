package com.ile.alert.messaging;

import aop.LogExecutionTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ile.alert.domain.snapshot.WeatherSnapshot;
import com.ile.alert.service.AlertProcessingService;
import dto.WeatherUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WeatherUpdateListener {

    private final AlertProcessingService alertProcessingService;
    private final ObjectMapper objectMapper;

    public WeatherUpdateListener(AlertProcessingService alertProcessingService,
                                 ObjectMapper objectMapper) {
        this.alertProcessingService = alertProcessingService;
        this.objectMapper = objectMapper;
    }

    @LogExecutionTime
    @KafkaListener(topics = "weather-updates", groupId = "alert-service")
    public void onWeatherUpdate(String message) {
        log.info("Received weather update message: {}", message);
        try {
            WeatherUpdateEvent event = objectMapper.readValue(message, WeatherUpdateEvent.class);

            WeatherSnapshot snapshot = new WeatherSnapshot();
            snapshot.setLocationName(event.locationName());
            snapshot.setTemperature(event.temperature());
            snapshot.setWindSpeed(event.windSpeed());
            snapshot.setPrecipitation(event.precipitation());
            snapshot.setHumidity(event.humidity());

            alertProcessingService.processWeatherUpdate(snapshot)
                    .subscribe(
                            null,
                            error -> log.error("Error processing weather update for {}: {}",
                                    event.locationName(), error.getMessage())
                    );
        } catch (Exception e) {
            log.error("Failed to deserialize weather update message: {}", e.getMessage());
        }
    }
}
