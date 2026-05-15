package com.ile.alert.domain.repository;

import com.ile.alert.domain.entity.AlertNotification;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface AlertNotificationRepository extends ReactiveCrudRepository<AlertNotification, UUID> {

    Flux<AlertNotification> findByUserIdOrderByTriggeredAtDesc(String userId);

    Mono<Long> countByUserIdAndIsRead(String userId, boolean isRead);
}