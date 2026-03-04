package com.enterprise.makerchecker.entity;

import com.enterprise.makerchecker.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "approval_request")
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private MakerCheckerConfig config;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status;

    // ── Original Request Details ──

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "request_path", nullable = false, length = 500)
    private String requestPath;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_headers", columnDefinition = "jsonb")
    private Map<String, String> requestHeaders;

    @Column(name = "query_params", columnDefinition = "TEXT")
    private String queryParams;

    // ── Maker Context ──

    @Column(name = "maker_user_id", nullable = false, length = 100)
    private String makerUserId;

    @Column(name = "maker_user_name", length = 200)
    private String makerUserName;

    @Column(name = "maker_roles", columnDefinition = "TEXT")
    private String makerRoles;

    @Column(name = "maker_product_code", length = 50)
    private String makerProductCode;

    // ── Checker Context ──

    @Column(name = "checker_user_id", length = 100)
    private String checkerUserId;

    @Column(name = "checker_user_name", length = 200)
    private String checkerUserName;

    @Column(name = "checker_comment", columnDefinition = "TEXT")
    private String checkerComment;

    // ── Execution Result ──

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    // ── Timestamps ──

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;
}
