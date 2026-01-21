package com.enterprise.notification.controller;

import com.enterprise.notification.dto.NotificationLogDTO;
import com.enterprise.notification.dto.NotificationTemplateDTO;
import com.enterprise.notification.dto.SendNotificationRequest;
import com.enterprise.notification.entity.NotificationLog.Status;
import com.enterprise.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ================== SEND ==================

    /**
     * Send a notification.
     */
    @PostMapping("/send")
    public ResponseEntity<NotificationLogDTO> send(@RequestBody SendNotificationRequest request) {
        NotificationLogDTO result = notificationService.send(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Send notification asynchronously.
     */
    @PostMapping("/send-async")
    public ResponseEntity<Map<String, String>> sendAsync(@RequestBody SendNotificationRequest request) {
        notificationService.sendAsync(request);
        return ResponseEntity.accepted().body(Map.of("message", "Notification queued for sending"));
    }

    // ================== TEMPLATES ==================

    /**
     * Get all active templates.
     */
    @GetMapping("/templates")
    public ResponseEntity<List<NotificationTemplateDTO>> getAllTemplates() {
        return ResponseEntity.ok(notificationService.getAllTemplates());
    }

    /**
     * Get template by code.
     */
    @GetMapping("/templates/{code}")
    public ResponseEntity<NotificationTemplateDTO> getTemplate(@PathVariable String code) {
        try {
            return ResponseEntity.ok(notificationService.getTemplate(code));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create a new template.
     */
    @PostMapping("/templates")
    public ResponseEntity<NotificationTemplateDTO> createTemplate(@RequestBody NotificationTemplateDTO request) {
        try {
            NotificationTemplateDTO result = notificationService.createTemplate(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ================== LOGS ==================

    /**
     * Get logs by recipient.
     */
    @GetMapping("/logs/recipient/{recipient}")
    public ResponseEntity<Page<NotificationLogDTO>> getLogsByRecipient(
            @PathVariable String recipient,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.getLogsByRecipient(recipient, pageable));
    }

    /**
     * Get logs by status.
     */
    @GetMapping("/logs/status/{status}")
    public ResponseEntity<Page<NotificationLogDTO>> getLogsByStatus(
            @PathVariable Status status,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.getLogsByStatus(status, pageable));
    }

    /**
     * Get logs by linked entity.
     */
    @GetMapping("/logs/entity/{entityType}/{entityId}")
    public ResponseEntity<List<NotificationLogDTO>> getLogsByEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        return ResponseEntity.ok(notificationService.getLogsByEntity(entityType, entityId));
    }
}
