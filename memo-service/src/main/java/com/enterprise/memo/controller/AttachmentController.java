package com.enterprise.memo.controller;

import com.enterprise.memo.entity.MemoAttachment;
import com.enterprise.memo.service.MemoAttachmentService;
import com.enterprise.memo.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/memos/{memoId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final MemoAttachmentService attachmentService;
    private final StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MemoAttachment> uploadAttachment(
            @PathVariable UUID memoId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(attachmentService.uploadAttachment(memoId, file));
    }

    @GetMapping
    public ResponseEntity<List<MemoAttachment>> getAttachments(@PathVariable UUID memoId) {
        return ResponseEntity.ok(attachmentService.getAttachments(memoId));
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable UUID memoId, // Not used but keeps URL structure
            @PathVariable UUID attachmentId) {

        // In real app, verify memoId matches attachment's memo
        MemoAttachment attachment = attachmentService.getAttachments(memoId).stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Attachment not found"));

        InputStreamResource resource = new InputStreamResource(
                storageService.downloadFile(attachment.getObjectName()));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .contentLength(attachment.getSize())
                .body(resource);
    }
}
