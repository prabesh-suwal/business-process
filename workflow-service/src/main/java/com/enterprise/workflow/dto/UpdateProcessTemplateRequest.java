package com.enterprise.workflow.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProcessTemplateRequest {

    private String name;
    private String description;
    private String bpmnXml;
}
