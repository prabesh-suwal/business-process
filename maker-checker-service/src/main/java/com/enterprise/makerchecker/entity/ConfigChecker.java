package com.enterprise.makerchecker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "config_checker")
@IdClass(ConfigChecker.ConfigCheckerId.class)
public class ConfigChecker {

    @Id
    @Column(name = "config_id", nullable = false)
    private UUID configId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigCheckerId implements Serializable {
        private UUID configId;
        private UUID userId;
    }
}
