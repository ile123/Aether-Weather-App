package dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AlertNotificationDto(
        UUID id,
        String userId,
        UUID ruleId,
        String message,
        boolean isRead,
        LocalDateTime triggeredAt,
        LocalDateTime createdAt
) {}