package com.enterprise.memo.service;

import com.enterprise.memo.client.DocumentServiceClient;
import com.enterprise.memo.entity.Memo;
import com.enterprise.memo.entity.MemoAttachment;
import com.enterprise.memo.event.DocumentAttachedEvent;
import com.enterprise.memo.repository.MemoAttachmentRepository;
import com.enterprise.memo.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemoAttachmentService {

        private final DocumentServiceClient documentServiceClient;
        private final MemoRepository memoRepository;
        private final MemoAttachmentRepository attachmentRepository;
        private final ApplicationEventPublisher eventPublisher;

        @Transactional
        public MemoAttachment uploadAttachment(UUID memoId, MultipartFile file, UUID userId, String userName) {
                Memo memo = memoRepository.findById(memoId)
                                .orElseThrow(() -> new RuntimeException("Memo not found"));

                // Upload to document-service
                Map<String, Object> docResponse = documentServiceClient.upload(
                                file, "MEMO", memoId);

                UUID documentId = UUID.fromString(docResponse.get("id").toString());
                String downloadUrl = docResponse.get("downloadUrl") != null
                                ? docResponse.get("downloadUrl").toString()
                                : null;

                // Save attachment record with document-service reference
                MemoAttachment attachment = MemoAttachment.builder()
                                .memo(memo)
                                .documentId(documentId)
                                .fileName(file.getOriginalFilename())
                                .contentType(file.getContentType())
                                .size(file.getSize())
                                .downloadUrl(downloadUrl)
                                .uploadedBy(userId)
                                .uploadedByName(userName)
                                .build();

                MemoAttachment saved = attachmentRepository.save(attachment);

                // Publish event for audit logging
                eventPublisher.publishEvent(new DocumentAttachedEvent(
                                this, memoId, memo.getMemoNumber(), documentId,
                                file.getOriginalFilename(), userId, userName,
                                DocumentAttachedEvent.Action.UPLOAD));

                log.info("Attachment '{}' uploaded for memo {} → documentId: {}",
                                file.getOriginalFilename(), memo.getMemoNumber(), documentId);

                return saved;
        }

        @Transactional(readOnly = true)
        public List<MemoAttachment> getAttachments(UUID memoId) {
                return attachmentRepository.findByMemoId(memoId);
        }

        @Transactional(readOnly = true)
        public MemoAttachment getAttachment(UUID attachmentId) {
                return attachmentRepository.findById(attachmentId)
                                .orElseThrow(() -> new RuntimeException("Attachment not found"));
        }

        @Transactional
        public void deleteAttachment(UUID memoId, UUID attachmentId, UUID userId, String userName) {
                MemoAttachment attachment = attachmentRepository.findById(attachmentId)
                                .orElseThrow(() -> new RuntimeException("Attachment not found"));

                if (!attachment.getMemo().getId().equals(memoId)) {
                        throw new RuntimeException("Attachment does not belong to this memo");
                }

                String fileName = attachment.getFileName();
                String memoNumber = attachment.getMemo().getMemoNumber();
                UUID documentId = attachment.getDocumentId();

                // Delete from document-service
                if (documentId != null) {
                        documentServiceClient.delete(documentId);
                }

                attachmentRepository.delete(attachment);

                // Publish event for audit logging
                eventPublisher.publishEvent(new DocumentAttachedEvent(
                                this, memoId, memoNumber, documentId,
                                fileName, userId, userName,
                                DocumentAttachedEvent.Action.DELETE));

                log.info("Attachment '{}' deleted from memo {}", fileName, memoNumber);
        }
}
