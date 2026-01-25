package com.enterprise.memo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTopicRequest {
    @NotNull
    private UUID categoryId;

    @NotBlank
    private String code; // e.g. "CAPEX"
    @NotBlank
    private String name; // e.g. "Capital Expense Request"
    private String description;

    private String workflowTemplateId;
    private UUID formDefinitionId;

    private Map<String, Object> contentTemplate; // Rich Text JSON

    @NotBlank
    private String numberingPattern; // e.g. "CPX-%FY%-%SEQ%"
}
