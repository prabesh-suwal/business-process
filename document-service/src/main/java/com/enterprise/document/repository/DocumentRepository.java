package com.enterprise.document.repository;

import com.enterprise.document.entity.Document;
import com.enterprise.document.entity.Document.DocumentType;
import com.enterprise.document.entity.Document.LinkedEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByLinkedEntityTypeAndLinkedEntityIdAndDeletedFalse(
            LinkedEntityType entityType, UUID entityId);

    List<Document> findByLinkedEntityTypeAndLinkedEntityIdAndDocumentTypeAndDeletedFalse(
            LinkedEntityType entityType, UUID entityId, DocumentType documentType);

    List<Document> findByUploadedByAndDeletedFalseOrderByUploadedAtDesc(UUID uploadedBy);

    Optional<Document> findByIdAndDeletedFalse(UUID id);

    long countByLinkedEntityTypeAndLinkedEntityIdAndDeletedFalse(
            LinkedEntityType entityType, UUID entityId);
}
