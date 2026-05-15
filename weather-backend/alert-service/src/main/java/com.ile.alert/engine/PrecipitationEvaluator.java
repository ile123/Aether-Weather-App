package com.ile.alert.engine;

import com.ile.alert.domain.entity.AlertRule;
import com.ile.alert.domain.snapshot.WeatherSnapshot;
import enums.AlertCondition;

import java.math.BigDecimal;

import static enums.AlertCondition.ABOVE;
import static enums.AlertCondition.EQUALS;

public class PrecipitationEvaluator implements RuleEvaluationStrategy {
    @Override
    public boolean evaluate(AlertRule rule, WeatherSnapshot snapshot) {
        return compare(snapshot.getPrecipitation(), rule.getThreshold(), rule.getAlertCondition());
    }

    private boolean compare(BigDecimal value, BigDecimal threshold, AlertCondition condition) {
        return switch (condition) {
            case ABOVE -> value.compareTo(threshold) > 0;
            case BELOW -> value.compareTo(threshold) < 0;
            case EQUALS -> value.compareTo(threshold) == 0;
        };
    }
}
