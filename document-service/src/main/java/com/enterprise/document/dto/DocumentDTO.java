package com.enterprise.document.dto;

import com.enterprise.document.entity.Document.DocumentType;
import com.enterprise.document.entity.Document.LinkedEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private UUID id;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private String storageType;
    private LinkedEntityType linkedEntityType;
    private UUID linkedEntityId;
    private DocumentType documentType;
    private String description;
    private UUID uploadedBy;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
}
