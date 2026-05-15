package com.ile.alert.domain.entity;

import enums.AlertCondition;
import enums.AlertType;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("alert_rules")
public class AlertRule implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("user_id")
    private String userId;

    @Column("location")
    private String location;

    @Column("type")
    private String type;

    @Column("condition")
    private String condition;

    @Column("threshold")
    private BigDecimal threshold;

    @Column("enabled")
    private boolean enabled;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @Transient
    @Builder.Default
    @Setter(AccessLevel.NONE)
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    public AlertType getAlertType() {
        return AlertType.valueOf(type);
    }

    public AlertCondition getAlertCondition() {
        return AlertCondition.valueOf(condition);
    }
}
