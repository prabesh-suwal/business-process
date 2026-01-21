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
}
