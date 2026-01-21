package com.enterprise.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteTaskRequest {

    private Map<String, Object> variables;

    private String comment;

    private boolean approved;
}
