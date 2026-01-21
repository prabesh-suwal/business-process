package com.enterprise.workflow.dto;

import com.enterprise.workflow.entity.ProcessTemplate.ProcessTemplateStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessTemplateDTO {

    private UUID id;
    private UUID productId;
    private String productName;
    private String name;
    private String description;
    private String flowableProcessDefKey;
    private String flowableDeploymentId;
    private Integer version;
    private ProcessTemplateStatus status;
    private String bpmnXml;
    private UUID createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
