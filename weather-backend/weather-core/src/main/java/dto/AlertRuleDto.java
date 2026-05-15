package dto;

import enums.AlertCondition;
import enums.AlertType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AlertRuleDto(
        UUID id,
        String userId,
        String location,
        AlertType type,
        AlertCondition condition,
        BigDecimal threshold,
        boolean enabled,
        LocalDateTime createdAt
) {}
