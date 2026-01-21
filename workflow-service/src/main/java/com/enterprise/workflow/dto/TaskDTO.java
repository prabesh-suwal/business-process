package com.enterprise.workflow.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDTO {

    private String id;
    private String name;
    private String description;
    private String processInstanceId;
    private UUID processTemplateId;
    private String processTemplateName;
    private UUID productId;
    private String productName;
    private String businessKey;
    private String processTitle;
    private String assignee;
    private String assigneeName;
    private String owner;
    private List<String> candidateGroups;
    private List<String> candidateUsers;
    private Integer priority;
    private LocalDateTime createTime;
    private LocalDateTime dueDate;
    private LocalDateTime claimTime;
    private String taskDefinitionKey;
    private UUID formDefinitionId;
    private Map<String, Object> processVariables;
    private Map<String, Object> taskLocalVariables;
}
