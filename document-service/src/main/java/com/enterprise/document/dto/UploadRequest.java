package com.enterprise.document.dto;

import com.enterprise.document.entity.Document.DocumentType;
import com.enterprise.document.entity.Document.LinkedEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadRequest {
    private LinkedEntityType linkedEntityType;
    private UUID linkedEntityId;
    private DocumentType documentType;
    private String description;
}
