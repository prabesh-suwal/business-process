package com.enterprise.memo.service;

import com.enterprise.memo.entity.Memo;
import com.enterprise.memo.entity.MemoAttachment;
import com.enterprise.memo.repository.MemoAttachmentRepository;
import com.enterprise.memo.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemoAttachmentService {

    private final StorageService storageService;
    private final MemoRepository memoRepository;
    private final MemoAttachmentRepository attachmentRepository;

    @Transactional
    public MemoAttachment uploadAttachment(UUID memoId, MultipartFile file) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new RuntimeException("Memo not found"));

        String objectName = memoId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        try {
            storageService.uploadFile(objectName, file.getInputStream(), file.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }

        MemoAttachment attachment = MemoAttachment.builder()
                .memo(memo)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .objectName(objectName)
                .build();

        return attachmentRepository.save(attachment);
    }

    @Transactional(readOnly = true)
    public List<MemoAttachment> getAttachments(UUID memoId) {
        return attachmentRepository.findByMemoId(memoId);
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId) {
        MemoAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));

        storageService.deleteFile(attachment.getObjectName());
        attachmentRepository.delete(attachment);
    }
}
