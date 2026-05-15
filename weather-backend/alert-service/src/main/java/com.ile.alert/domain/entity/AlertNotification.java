package com.ile.alert.domain.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("alert_notifications")
public class AlertNotification implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("user_id")
    private String userId;

    @Column("rule_id")
    private UUID ruleId;

    @Column("message")
    private String message;

    @Column("is_read")
    private boolean isRead;

    @Column("triggered_at")
    private LocalDateTime triggeredAt;

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
}