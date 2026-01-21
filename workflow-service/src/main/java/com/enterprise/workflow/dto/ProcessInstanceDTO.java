package com.enterprise.workflow.dto;

import com.enterprise.workflow.entity.ProcessInstanceMetadata.ProcessInstanceStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessInstanceDTO {

    private UUID id;
    private String flowableProcessInstanceId;
    private UUID processTemplateId;
    private String processTemplateName;
    private UUID productId;
    private String productName;
    private String businessKey;
    private String title;
    private UUID startedBy;
    private String startedByName;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private ProcessInstanceStatus status;
    private Integer priority;
    private LocalDateTime dueDate;
    private Map<String, Object> variables;
    private String currentTaskName;
    private String currentAssignee;
}
