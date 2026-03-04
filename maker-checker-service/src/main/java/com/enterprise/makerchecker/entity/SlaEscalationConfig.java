package com.enterprise.makerchecker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sla_escalation_config", uniqueConstraints = @UniqueConstraint(columnNames = { "config_id" }))
public class SlaEscalationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private MakerCheckerConfig config;

    @Column(name = "deadline_hours", nullable = false)
    private int deadlineHours;

    @Column(name = "escalation_role", length = 100)
    private String escalationRole;

    @Column(name = "auto_expire", nullable = false)
    private boolean autoExpire;
}
