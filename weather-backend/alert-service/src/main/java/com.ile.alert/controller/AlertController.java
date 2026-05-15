package com.ile.alert.controller;

import com.ile.alert.service.AlertNotificationService;
import com.ile.alert.service.AlertRuleService;
import dto.AlertNotificationDto;
import dto.AlertRuleDto;
import dto.CreateAlertRuleRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@Validated
public class AlertController {

    private final AlertRuleService alertRuleService;
    private final AlertNotificationService alertNotificationService;

    public AlertController(AlertRuleService alertRuleService,
                           AlertNotificationService alertNotificationService) {
        this.alertRuleService = alertRuleService;
        this.alertNotificationService = alertNotificationService;
    }

    @GetMapping("/rules")
    public Flux<AlertRuleDto> getRules(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        return alertRuleService.getRulesForUser(userId);
    }

    @PostMapping("/rules")
    public Mono<ResponseEntity<AlertRuleDto>> createRule(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
            @RequestBody @Valid CreateAlertRuleRequest request) {
        return alertRuleService.createRule(userId, request)
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @DeleteMapping("/rules/{id}")
    public Mono<ResponseEntity<Void>> deleteRule(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
            @PathVariable UUID id) {
        return alertRuleService.deleteRule(userId, id)
                .thenReturn(ResponseEntity.<Void>noContent().build());
    }

    @GetMapping("/notifications")
    public Flux<AlertNotificationDto> getNotifications(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        return alertNotificationService.getNotificationsForUser(userId);
    }

    @PutMapping("/notifications/{id}/read")
    public Mono<ResponseEntity<AlertNotificationDto>> markAsRead(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
            @PathVariable UUID id) {
        return alertNotificationService.markAsRead(userId, id)
                .map(ResponseEntity::ok);
    }
}
