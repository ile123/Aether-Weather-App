package com.ile.alert.engine;

import com.ile.alert.domain.entity.AlertRule;
import com.ile.alert.domain.snapshot.WeatherSnapshot;
import enums.AlertCondition;

public class TemperatureEvaluator implements RuleEvaluationStrategy {
    @Override
    public boolean evaluate(AlertRule rule, WeatherSnapshot snapshot) {
        return compare(snapshot.getTemperature(), rule.getThreshold(), rule.getAlertCondition());
    }

    private boolean compare(java.math.BigDecimal value, java.math.BigDecimal threshold, AlertCondition condition) {
        return switch (condition) {
            case ABOVE -> value.compareTo(threshold) > 0;
            case BELOW -> value.compareTo(threshold) < 0;
            case EQUALS -> value.compareTo(threshold) == 0;
        };
    }
}
