package com.enterprise.document.controller;

import com.enterprise.document.dto.DocumentDTO;
import com.enterprise.document.dto.UploadRequest;
import com.enterprise.document.entity.Document.DocumentType;
import com.enterprise.document.entity.Document.LinkedEntityType;
import com.enterprise.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Upload a document.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "linkedEntityType", required = false) LinkedEntityType linkedEntityType,
            @RequestParam(value = "linkedEntityId", required = false) UUID linkedEntityId,
            @RequestParam(value = "documentType", required = false) DocumentType documentType,
            @RequestParam(value = "description", required = false) String description,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        UploadRequest request = UploadRequest.builder()
                .linkedEntityType(linkedEntityType)
                .linkedEntityId(linkedEntityId)
                .documentType(documentType != null ? documentType : DocumentType.OTHER)
                .description(description)
                .build();

        UUID uploadedBy = userId != null ? UUID.fromString(userId) : null;
        String uploadedByName = userName != null ? userName : "System";

        DocumentDTO result = documentService.upload(file, request, uploadedBy, uploadedByName);
        return ResponseEntity.ok(result);
    }

    /**
     * Get document metadata.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocument(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(documentService.getDocument(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Download document file.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
        try {
            DocumentDTO doc = documentService.getDocument(id);
            InputStream inputStream = documentService.download(id);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + doc.getOriginalFilename() + "\"")
                    .contentType(MediaType.parseMediaType(doc.getContentType()))
                    .contentLength(doc.getFileSize())
                    .body(new InputStreamResource(inputStream));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get documents by linked entity.
     */
    @GetMapping("/by-entity/{entityType}/{entityId}")
    public ResponseEntity<List<DocumentDTO>> getByEntity(
            @PathVariable LinkedEntityType entityType,
            @PathVariable UUID entityId) {
        return ResponseEntity.ok(documentService.getByEntity(entityType, entityId));
    }

    /**
     * Get documents by linked entity and document type.
     */
    @GetMapping("/by-entity/{entityType}/{entityId}/type/{docType}")
    public ResponseEntity<List<DocumentDTO>> getByEntityAndType(
            @PathVariable LinkedEntityType entityType,
            @PathVariable UUID entityId,
            @PathVariable DocumentType docType) {
        return ResponseEntity.ok(documentService.getByEntityAndType(entityType, entityId, docType));
    }

    /**
     * Delete a document (soft delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            UUID deletedBy = userId != null ? UUID.fromString(userId) : null;
            documentService.delete(id, deletedBy);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
