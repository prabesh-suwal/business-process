package com.enterprise.form.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadDTO {

    private UUID id;
    private String fieldKey;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private String downloadUrl;
    private LocalDateTime uploadedAt;
}
