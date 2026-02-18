package com.enterprise.memo.controller;

import com.cas.common.security.UserContext;
import com.cas.common.security.UserContextHolder;
import com.enterprise.memo.client.DocumentServiceClient;
import com.enterprise.memo.entity.MemoAttachment;
import com.enterprise.memo.service.MemoAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/memos/{memoId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final MemoAttachmentService attachmentService;
    private final DocumentServiceClient documentServiceClient;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MemoAttachment> uploadAttachment(
            @PathVariable UUID memoId,
            @RequestParam("file") MultipartFile file) {
        UserContext user = UserContextHolder.require();
        UUID userId = UUID.fromString(user.getUserId());
        String userName = user.getName();
        return ResponseEntity.ok(attachmentService.uploadAttachment(memoId, file, userId, userName));
    }

    @GetMapping
    public ResponseEntity<List<MemoAttachment>> getAttachments(@PathVariable UUID memoId) {
        return ResponseEntity.ok(attachmentService.getAttachments(memoId));
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID memoId,
            @PathVariable UUID attachmentId) {
        UserContext user = UserContextHolder.require();
        UUID userId = UUID.fromString(user.getUserId());
        String userName = user.getName();
        attachmentService.deleteAttachment(memoId, attachmentId, userId, userName);
        return ResponseEntity.noContent().build();
    }

    /**
     * Proxy download through document-service.
     * This keeps the API surface consistent under
     * /api/memos/{memoId}/attachments/*.
     */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable UUID memoId,
            @PathVariable UUID attachmentId) {

        MemoAttachment attachment = attachmentService.getAttachment(attachmentId);

        if (!attachment.getMemo().getId().equals(memoId)) {
            return ResponseEntity.notFound().build();
        }

        // Proxy the download from document-service
        InputStream stream = documentServiceClient.download(attachment.getDocumentId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(
                        attachment.getContentType() != null ? attachment.getContentType() : "application/octet-stream"))
                .contentLength(attachment.getSize() != null ? attachment.getSize() : -1)
                .body(new InputStreamResource(stream));
    }
}
