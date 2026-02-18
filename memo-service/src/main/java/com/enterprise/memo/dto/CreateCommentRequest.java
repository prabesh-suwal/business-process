package com.enterprise.memo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateCommentRequest {
    @NotBlank(message = "Comment content is required")
    private String content;

    private UUID parentCommentId; // Optional - for replies

    private List<String> mentionedUserIds; // Optional - tagged user IDs
}
