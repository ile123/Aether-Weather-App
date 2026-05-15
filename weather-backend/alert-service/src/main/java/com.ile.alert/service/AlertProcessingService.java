package com.ile.alert.service;

import aop.Audited;
import com.ile.alert.domain.entity.AlertNotification;
import com.ile.alert.domain.repository.AlertNotificationRepository;
import com.ile.alert.domain.repository.AlertRuleRepository;
import com.ile.alert.domain.snapshot.WeatherSnapshot;
import com.ile.alert.engine.AlertRuleEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class AlertProcessingService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertNotificationRepository alertNotificationRepository;
    private final AlertRuleEvaluator alertRuleEvaluator;

    public AlertProcessingService(AlertRuleRepository alertRuleRepository, AlertNotificationRepository alertNotificationRepository, AlertRuleEvaluator alertRuleEvaluator) {
        this.alertRuleRepository = alertRuleRepository;
        this.alertNotificationRepository = alertNotificationRepository;
        this.alertRuleEvaluator = alertRuleEvaluator;
    }

    @Audited(action = "ALERT_TRIGGERED")
    public Mono<Void> processWeatherUpdate(WeatherSnapshot weatherSnapshot) {
        log.info("Processing weather update for location: {}", weatherSnapshot.getLocationName());
        return alertRuleRepository
                .findByLocationAndEnabled(weatherSnapshot.getLocationName(), true)
                .collectList()
                .flatMapMany(rules -> {
                    log.info("Found {} rules for location: {}", rules.size(), weatherSnapshot.getLocationName());
                    return alertRuleEvaluator.evaluateAll(rules, weatherSnapshot);
                })
                .flatMap(rule -> {
                    var message = String.format(
                            "%s in %s is %s which is %s your threshold of %s",
                            rule.getAlertType().name(),
                            weatherSnapshot.getLocationName(),
                            getValueForType(rule.getAlertType(), weatherSnapshot),
                            rule.getAlertCondition().name(),
                            rule.getThreshold()
                    );

                    var notification = AlertNotification.builder()
                            .id(UUID.randomUUID())
                            .userId(rule.getUserId())
                            .ruleId(rule.getId())
                            .message(message)
                            .isRead(false)
                            .triggeredAt(LocalDateTime.now())
                            .build();

                    return alertNotificationRepository.save(notification);
                })
                .then();
    }

    private String getValueForType(enums.AlertType type, WeatherSnapshot snapshot) {
        return switch (type) {
            case TEMPERATURE -> String.valueOf(snapshot.getTemperature());
            case WIND -> String.valueOf(snapshot.getWindSpeed());
            case PRECIPITATION -> String.valueOf(snapshot.getPrecipitation());
            case HUMIDITY -> String.valueOf(snapshot.getHumidity());
        };
    }
}
