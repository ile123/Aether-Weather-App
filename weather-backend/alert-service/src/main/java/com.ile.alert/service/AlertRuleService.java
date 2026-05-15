package com.ile.alert.service;

import com.ile.alert.domain.entity.AlertRule;
import com.ile.alert.domain.repository.AlertRuleRepository;
import dto.AlertRuleDto;
import dto.CreateAlertRuleRequest;
import exception.ResourceNotFoundException;
import exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;

    public AlertRuleService(AlertRuleRepository alertRuleRepository) {
        this.alertRuleRepository = alertRuleRepository;
    }

    public Mono<AlertRuleDto> createRule(String userId, CreateAlertRuleRequest request) {
        var rule = AlertRule.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .location(request.location())
                .type(request.type().name())
                .condition(request.condition().name())
                .threshold(request.threshold())
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        return alertRuleRepository.save(rule)
                .map(this::toDto);
    }

    public Flux<AlertRuleDto> getRulesForUser(String userId) {
        return alertRuleRepository.findByUserId(userId)
                .map(this::toDto);
    }

    public Mono<Void> deleteRule(String userId, UUID ruleId) {
        return alertRuleRepository.findById(ruleId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Rule not found: ", ruleId.toString())))
                .flatMap(rule -> {
                    if (!rule.getUserId().equals(userId)) {
                        return Mono.error(new ValidationException("Rule error: ", "You do not own this rule"));
                    }
                    return alertRuleRepository.deleteById(ruleId);
                });
    }

    private AlertRuleDto toDto(AlertRule rule) {
        return new AlertRuleDto(
                rule.getId(),
                rule.getUserId(),
                rule.getLocation(),
                rule.getAlertType(),
                rule.getAlertCondition(),
                rule.getThreshold(),
                rule.isEnabled(),
                rule.getCreatedAt()
        );
    }
}