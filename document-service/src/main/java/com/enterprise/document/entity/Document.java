package com.enterprise.document.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Document entity - stores metadata about uploaded files.
 * Actual file content is stored in configured storage (local/MinIO).
 */
@Entity
@Table(name = "document", indexes = {
        @Index(name = "idx_document_entity", columnList = "linked_entity_type, linked_entity_id"),
        @Index(name = "idx_document_uploaded_by", columnList = "uploaded_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename; // UUID-based filename in storage

    @Column(name = "content_type")
    private String contentType; // MIME type

    @Column(name = "file_size")
    private Long fileSize; // bytes

    @Column(name = "storage_path", nullable = false)
    private String storagePath; // Path/key in storage

    @Column(name = "storage_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private StorageType storageType;

    // Linking to business entities
    @Column(name = "linked_entity_type")
    @Enumerated(EnumType.STRING)
    private LinkedEntityType linkedEntityType;

    @Column(name = "linked_entity_id")
    private UUID linkedEntityId;

    // Document classification
    @Column(name = "document_type")
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(name = "description")
    private String description;

    // Audit fields
    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "uploaded_by_name")
    private String uploadedByName;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted")
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    // Enums
    public enum StorageType {
        LOCAL,
        MINIO,
        S3
    }

    public enum LinkedEntityType {
        LOAN_APPLICATION,
        TASK,
        COLLATERAL,
        CUSTOMER,
        PROCESS_INSTANCE,
        FORM_SUBMISSION,
        MEMO,
        OTHER
    }

    public enum DocumentType {
        // Identity documents
        ID_PROOF,
        ADDRESS_PROOF,
        PHOTO,

        // Income documents
        SALARY_SLIP,
        BANK_STATEMENT,
        ITR,
        FORM_16,

        // Property documents
        PROPERTY_DEED,
        TITLE_DOCUMENT,
        VALUATION_REPORT,
        LEGAL_OPINION,

        // Vehicle documents
        RC_BOOK,
        INSURANCE,

        // Loan documents
        SANCTION_LETTER,
        LOAN_AGREEMENT,
        SIGNED_APPLICATION,

        // Other
        OTHER
    }
}
