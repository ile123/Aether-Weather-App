package com.ile.alert.engine;

import com.ile.alert.domain.entity.AlertRule;
import com.ile.alert.domain.snapshot.WeatherSnapshot;
import enums.AlertType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AlertRuleEvaluator {

    private final Map<AlertType, RuleEvaluationStrategy> strategies;

    public AlertRuleEvaluator() {
        this.strategies = Map.of(
                AlertType.TEMPERATURE, new TemperatureEvaluator(),
                AlertType.WIND, new WindEvaluator(),
                AlertType.PRECIPITATION, new PrecipitationEvaluator(),
                AlertType.HUMIDITY, new HumidityEvaluator()
        );
    }

    public Flux<AlertRule> evaluateAll(List<AlertRule> rules, WeatherSnapshot snapshot) {
        return Flux.fromIterable(rules)
                .filter(rule -> {
                    RuleEvaluationStrategy strategy = strategies.get(rule.getAlertType());
                    if (strategy == null) {
                        log.warn("No strategy found for alert type: {}", rule.getAlertType());
                        return false;
                    }
                    return strategy.evaluate(rule, snapshot);
                });
    }
}
