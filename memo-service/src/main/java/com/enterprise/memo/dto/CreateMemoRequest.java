package com.enterprise.memo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateMemoRequest {
    @NotNull(message = "Topic ID is required")
    private UUID topicId;

    @NotNull(message = "Subject is required")
    private String subject;

    // Initial priority
    private String priority = "NORMAL";
}
