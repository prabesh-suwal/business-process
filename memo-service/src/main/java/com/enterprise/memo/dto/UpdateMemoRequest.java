package com.enterprise.memo.dto;

import lombok.Data;
import java.util.Map;

@Data
public class UpdateMemoRequest {
    private String subject;
    private String priority;

    // Rich text content update
    private Map<String, Object> content;

    // Form data update
    private Map<String, Object> formData;

    // Custom workflow overrides (when user customizes workflow for this memo)
    private Map<String, Object> workflowOverrides;
}
