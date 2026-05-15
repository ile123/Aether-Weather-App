package dto;

import enums.AlertCondition;
import enums.AlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateAlertRuleRequest(
        @NotBlank String location,
        @NotNull AlertType type,
        @NotNull AlertCondition condition,
        @NotNull @Positive BigDecimal threshold
) {}
