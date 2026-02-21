package com.enterprise.memo.dto;

import com.enterprise.memo.entity.MemoStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class MemoDTO {
    private UUID id;
    private String memoNumber;
    private String subject;
    private MemoStatus status;
    private String priority;

    private Map<String, Object> content;
    private Map<String, Object> formData;

    private UUID topicId;
    private String topicName;
    private UUID categoryId;
    private String categoryName;

    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String processInstanceId;
}
