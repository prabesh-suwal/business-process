package com.enterprise.form.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * File upload metadata for form submissions.
 */
@Entity
@Table(name = "file_upload", indexes = {
        @Index(name = "idx_file_upload_submission", columnList = "form_submission_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_submission_id")
    private FormSubmission formSubmission;

    @Column(name = "field_key", nullable = false)
    private String fieldKey;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 500)
    private String storedFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "minio_bucket")
    private String minioBucket;

    @Column(name = "minio_object_key", length = 500)
    private String minioObjectKey;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;
}
