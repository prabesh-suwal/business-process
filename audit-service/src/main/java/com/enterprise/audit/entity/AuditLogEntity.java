package com.enterprise.audit.entity;

import com.cas.common.audit.ActorType;
import com.cas.common.audit.AuditAction;
import com.cas.common.audit.AuditCategory;
import com.cas.common.audit.AuditResult;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Immutable audit log entity with hash chain for tamper evidence.
 * Once persisted, these records cannot be modified or deleted.
 * Each record contains a hash of its content and the previous record's hash,
 * forming a blockchain-like chain that ensures integrity.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_correlation_id", columnList = "correlation_id"),
        @Index(name = "idx_audit_actor_id", columnList = "actor_id"),
        @Index(name = "idx_audit_resource", columnList = "resource_type, resource_id"),
        @Index(name = "idx_audit_category", columnList = "category"),
        @Index(name = "idx_audit_service", columnList = "service_name"),
        @Index(name = "idx_audit_product", columnList = "product_code")
})
@Immutable // JPA-level immutability
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "sequence_number", unique = true, nullable = false, updatable = false)
    private Long sequenceNumber;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    // Actor Information
    @Column(name = "actor_id", length = 100, updatable = false)
    private String actorId;

    @Column(name = "actor_name", length = 255, updatable = false)
    private String actorName;

    @Column(name = "actor_email", length = 255, updatable = false)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", length = 50, updatable = false)
    private ActorType actorType;

    @Column(name = "actor_roles", length = 1000, updatable = false)
    private String actorRoles; // Comma-separated roles

    @Column(name = "ip_address", length = 50, updatable = false)
    private String ipAddress;

    // Action Information
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, updatable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, updatable = false)
    private AuditCategory category;

    // Resource Information
    @Column(name = "resource_type", length = 100, updatable = false)
    private String resourceType;

    @Column(name = "resource_id", length = 100, updatable = false)
    private String resourceId;

    // Context
    @Column(length = 2000, updatable = false)
    private String description;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String metadata; // JSON string for flexible metadata

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private AuditResult result;

    @Column(name = "error_message", length = 2000, updatable = false)
    private String errorMessage;

    // Source Information
    @Column(name = "service_name", length = 100, updatable = false)
    private String serviceName;

    @Column(name = "product_code", length = 50, updatable = false)
    private String productCode;

    @Column(name = "correlation_id", length = 50, updatable = false)
    private String correlationId;

    // Tamper Evidence - Hash Chain
    @Column(name = "previous_hash", length = 64, updatable = false)
    private String previousHash; // SHA-256 hash of previous record

    @Column(name = "record_hash", nullable = false, length = 64, updatable = false)
    private String recordHash; // SHA-256 hash of this record's content

    /**
     * Calculates the SHA-256 hash of this record's content.
     * The hash includes all immutable fields to detect any tampering.
     */
    public String calculateContentHash() {
        String content = String.join("|",
                nullSafe(sequenceNumber),
                nullSafe(timestamp),
                nullSafe(actorId),
                nullSafe(actorName),
                nullSafe(actorEmail),
                nullSafe(actorType),
                nullSafe(actorRoles),
                nullSafe(ipAddress),
                nullSafe(action),
                nullSafe(category),
                nullSafe(resourceType),
                nullSafe(resourceId),
                nullSafe(description),
                nullSafe(metadata),
                nullSafe(result),
                nullSafe(errorMessage),
                nullSafe(serviceName),
                nullSafe(productCode),
                nullSafe(correlationId),
                nullSafe(previousHash));
        return sha256(content);
    }

    /**
     * Verifies that the record hash matches the calculated hash.
     * Used for integrity checking.
     */
    public boolean verifyIntegrity() {
        return recordHash != null && recordHash.equals(calculateContentHash());
    }

    private static String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Helper to convert comma-separated roles back to list
     */
    public List<String> getActorRolesList() {
        if (actorRoles == null || actorRoles.isBlank()) {
            return List.of();
        }
        return List.of(actorRoles.split(","));
    }
}
