package com.enterprise.memo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private UUID id;
    private UUID memoId;
    private UUID parentCommentId;
    private UUID userId;
    private String userName;
    private String content;
    private String type;
    private List<String> mentionedUserIds;
    private LocalDateTime createdAt;
    private List<CommentDTO> replies;
}
