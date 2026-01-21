package com.enterprise.form.repository;

import com.enterprise.form.entity.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, UUID> {

    List<FileUpload> findByFormSubmissionId(UUID formSubmissionId);

    List<FileUpload> findByFormSubmissionIdAndFieldKey(UUID formSubmissionId, String fieldKey);
}
