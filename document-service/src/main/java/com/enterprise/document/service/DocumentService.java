package com.enterprise.document.service;

import com.enterprise.document.dto.DocumentDTO;
import com.enterprise.document.dto.UploadRequest;
import com.enterprise.document.entity.Document;
import com.enterprise.document.entity.Document.*;
import com.enterprise.document.repository.DocumentRepository;
import com.enterprise.document.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final Tika tika = new Tika();

    /**
     * Upload a document.
     */
    public DocumentDTO upload(MultipartFile file, UploadRequest request, UUID uploadedBy, String uploadedByName) {
        // Generate stored filename
        String extension = getFileExtension(file.getOriginalFilename());
        String storedFilename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        // Detect content type
        String contentType = detectContentType(file);

        // Store file
        String storagePath = storageService.store(file, storedFilename);

        // Create document record
        Document document = Document.builder()
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedFilename)
                .contentType(contentType)
                .fileSize(file.getSize())
                .storagePath(storagePath)
                .storageType(StorageType.valueOf(storageService.getStorageType()))
                .linkedEntityType(request.getLinkedEntityType())
                .linkedEntityId(request.getLinkedEntityId())
                .documentType(request.getDocumentType())
                .description(request.getDescription())
                .uploadedBy(uploadedBy)
                .uploadedByName(uploadedByName)
                .build();

        document = documentRepository.save(document);
        log.info("Uploaded document: {} ({})", document.getOriginalFilename(), document.getId());

        return toDTO(document);
    }

    /**
     * Get document metadata by ID.
     */
    @Transactional(readOnly = true)
    public DocumentDTO getDocument(UUID documentId) {
        Document document = documentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        return toDTO(document);
    }

    /**
     * Download document content.
     */
    @Transactional(readOnly = true)
    public InputStream download(UUID documentId) {
        Document document = documentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        return storageService.retrieve(document.getStoragePath());
    }

    /**
     * Get documents by linked entity.
     */
    @Transactional(readOnly = true)
    public List<DocumentDTO> getByEntity(LinkedEntityType entityType, UUID entityId) {
        return documentRepository.findByLinkedEntityTypeAndLinkedEntityIdAndDeletedFalse(entityType, entityId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get documents by linked entity and document type.
     */
    @Transactional(readOnly = true)
    public List<DocumentDTO> getByEntityAndType(LinkedEntityType entityType, UUID entityId, DocumentType docType) {
        return documentRepository.findByLinkedEntityTypeAndLinkedEntityIdAndDocumentTypeAndDeletedFalse(
                entityType, entityId, docType)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Soft delete a document.
     */
    public void delete(UUID documentId, UUID deletedBy) {
        Document document = documentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        document.setDeleted(true);
        document.setDeletedAt(LocalDateTime.now());
        document.setDeletedBy(deletedBy);
        documentRepository.save(document);

        log.info("Soft deleted document: {}", documentId);
    }

    /**
     * Permanently delete a document (also removes from storage).
     */
    public void permanentDelete(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        storageService.delete(document.getStoragePath());
        documentRepository.delete(document);

        log.info("Permanently deleted document: {}", documentId);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String detectContentType(MultipartFile file) {
        try {
            return tika.detect(file.getInputStream());
        } catch (IOException e) {
            return file.getContentType();
        }
    }

    private DocumentDTO toDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .originalFilename(document.getOriginalFilename())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .storageType(document.getStorageType().name())
                .linkedEntityType(document.getLinkedEntityType())
                .linkedEntityId(document.getLinkedEntityId())
                .documentType(document.getDocumentType())
                .description(document.getDescription())
                .uploadedBy(document.getUploadedBy())
                .uploadedByName(document.getUploadedByName())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}
