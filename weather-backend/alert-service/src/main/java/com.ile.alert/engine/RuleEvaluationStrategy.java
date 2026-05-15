package com.ile.alert.engine;

import com.ile.alert.domain.entity.AlertRule;
import com.ile.alert.domain.snapshot.WeatherSnapshot;

public interface RuleEvaluationStrategy {
    boolean evaluate(AlertRule rule, WeatherSnapshot snapshot);
}
