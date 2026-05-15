package com.ile.alert.domain.repository;

import com.ile.alert.domain.entity.AlertRule;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface AlertRuleRepository extends ReactiveCrudRepository<AlertRule, UUID> {

    Flux<AlertRule> findByUserId(String userId);

    Flux<AlertRule> findByLocationAndEnabled(String location, boolean enabled);
}