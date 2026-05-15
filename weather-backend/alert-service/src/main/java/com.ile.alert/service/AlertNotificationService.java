package com.ile.alert.service;

import com.ile.alert.domain.repository.AlertNotificationRepository;
import dto.AlertNotificationDto;
import exception.ResourceNotFoundException;
import exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
public class AlertNotificationService {

    private final AlertNotificationRepository alertNotificationRepository;

    public AlertNotificationService(AlertNotificationRepository alertNotificationRepository) {
        this.alertNotificationRepository = alertNotificationRepository;
    }

    public Flux<AlertNotificationDto> getNotificationsForUser(String userId) {
        return alertNotificationRepository.findByUserIdOrderByTriggeredAtDesc(userId)
                .map(this::toDto);
    }

    public Mono<AlertNotificationDto> markAsRead(String userId, UUID notificationId) {
        return alertNotificationRepository.findById(notificationId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Notification not found: ", notificationId.toString())))
                .flatMap(notification -> {
                    if (!notification.getUserId().equals(userId)) {
                        return Mono.error(new ValidationException("Notification error: ", "You do not own this notification"));
                    }
                    notification.setRead(true);
                    return alertNotificationRepository.save(notification);
                })
                .map(this::toDto);
    }

    public Mono<Long> getUnreadCount(String userId) {
        return alertNotificationRepository.countByUserIdAndIsRead(userId, false);
    }

    private AlertNotificationDto toDto(com.ile.alert.domain.entity.AlertNotification n) {
        return new AlertNotificationDto(
                n.getId(),
                n.getUserId(),
                n.getRuleId(),
                n.getMessage(),
                n.isRead(),
                n.getTriggeredAt(),
                n.getCreatedAt()
        );
    }
}
