package com.enterprise.workflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DecisionTableDTO {

    private UUID id;
    private String name;
    private String key;
    private String description;
    private String dmnXml;
    private Integer version;
    private String status;
    private UUID productId;
    private String productCode;
    private String flowableDeploymentId;
    private String flowableDecisionKey;
    private UUID previousVersionId;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private UUID createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
