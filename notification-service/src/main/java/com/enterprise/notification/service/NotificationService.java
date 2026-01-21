package com.enterprise.notification.service;

import com.enterprise.notification.dto.NotificationLogDTO;
import com.enterprise.notification.dto.NotificationTemplateDTO;
import com.enterprise.notification.dto.SendNotificationRequest;
import com.enterprise.notification.entity.NotificationLog;
import com.enterprise.notification.entity.NotificationLog.Status;
import com.enterprise.notification.entity.NotificationTemplate;
import com.enterprise.notification.entity.NotificationTemplate.Channel;
import com.enterprise.notification.repository.NotificationLogRepository;
import com.enterprise.notification.repository.NotificationTemplateRepository;
import com.enterprise.notification.service.sender.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final List<NotificationSender> senders;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    // ================== SEND NOTIFICATIONS ==================

    /**
     * Send a notification (async).
     */
    @Async
    public void sendAsync(SendNotificationRequest request) {
        try {
            send(request);
        } catch (Exception e) {
            log.error("Failed to send notification asynchronously: {}", e.getMessage());
        }
    }

    /**
     * Send a notification (sync).
     */
    public NotificationLogDTO send(SendNotificationRequest request) {
        NotificationLog notification = createNotificationLog(request);
        notification = logRepository.save(notification);

        try {
            notification.setStatus(Status.SENDING);
            logRepository.save(notification);

            // Find appropriate sender
            NotificationSender sender = findSender(notification.getChannel());
            if (sender == null || !sender.isEnabled()) {
                throw new RuntimeException("No enabled sender for channel: " + notification.getChannel());
            }

            sender.send(notification);

            notification.setStatus(Status.SENT);
            notification.setSentAt(LocalDateTime.now());
            logRepository.save(notification);

            log.info("Notification sent: {} to {} via {}",
                    notification.getId(), notification.getRecipient(), notification.getChannel());

        } catch (Exception e) {
            notification.setStatus(Status.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.incrementRetry();
            logRepository.save(notification);

            log.error("Notification failed: {} - {}", notification.getId(), e.getMessage());
        }

        return toLogDTO(notification);
    }

    /**
     * Send notification by event (finds all templates for the event).
     */
    public void sendByEvent(NotificationTemplate.TriggerEvent event, String recipient,
            String recipientName, Map<String, Object> variables,
            String linkedEntityType, UUID linkedEntityId) {

        List<NotificationTemplate> templates = templateRepository.findByTriggerEventAndActiveTrue(event);

        for (NotificationTemplate template : templates) {
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .templateCode(template.getCode())
                    .channel(template.getChannel())
                    .recipient(recipient)
                    .recipientName(recipientName)
                    .variables(variables)
                    .linkedEntityType(linkedEntityType)
                    .linkedEntityId(linkedEntityId)
                    .triggerEvent(event)
                    .build();

            sendAsync(request);
        }
    }

    // ================== TEMPLATE MANAGEMENT ==================

    public NotificationTemplateDTO createTemplate(NotificationTemplateDTO dto) {
        if (templateRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Template code already exists: " + dto.getCode());
        }

        NotificationTemplate template = NotificationTemplate.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .description(dto.getDescription())
                .channel(dto.getChannel())
                .subjectTemplate(dto.getSubjectTemplate())
                .bodyTemplate(dto.getBodyTemplate())
                .triggerEvent(dto.getTriggerEvent())
                .productId(dto.getProductId())
                .expectedVariables(dto.getExpectedVariables())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        template = templateRepository.save(template);
        log.info("Created notification template: {}", template.getCode());
        return toTemplateDTO(template);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateDTO> getAllTemplates() {
        return templateRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toTemplateDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotificationTemplateDTO getTemplate(String code) {
        return templateRepository.findByCodeAndActiveTrue(code)
                .map(this::toTemplateDTO)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + code));
    }

    // ================== LOG QUERIES ==================

    @Transactional(readOnly = true)
    public Page<NotificationLogDTO> getLogsByRecipient(String recipient, Pageable pageable) {
        return logRepository.findByRecipientOrderByCreatedAtDesc(recipient, pageable)
                .map(this::toLogDTO);
    }

    @Transactional(readOnly = true)
    public Page<NotificationLogDTO> getLogsByStatus(Status status, Pageable pageable) {
        return logRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(this::toLogDTO);
    }

    @Transactional(readOnly = true)
    public List<NotificationLogDTO> getLogsByEntity(String entityType, UUID entityId) {
        return logRepository.findByLinkedEntityTypeAndLinkedEntityId(entityType, entityId)
                .stream()
                .map(this::toLogDTO)
                .collect(Collectors.toList());
    }

    // ================== HELPERS ==================

    private NotificationLog createNotificationLog(SendNotificationRequest request) {
        NotificationTemplate template = null;
        String subject = request.getSubject();
        String body = request.getBody();
        Channel channel = request.getChannel();

        // If template code provided, load and render
        if (request.getTemplateCode() != null) {
            template = templateRepository.findByCodeAndActiveTrue(request.getTemplateCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Template not found: " + request.getTemplateCode()));

            subject = renderTemplate(template.getSubjectTemplate(), request.getVariables());
            body = renderTemplate(template.getBodyTemplate(), request.getVariables());
            channel = template.getChannel();
        }

        return NotificationLog.builder()
                .template(template)
                .templateCode(template != null ? template.getCode() : null)
                .channel(channel)
                .recipient(request.getRecipient())
                .recipientName(request.getRecipientName())
                .subject(subject)
                .body(body)
                .variables(request.getVariables())
                .linkedEntityType(request.getLinkedEntityType())
                .linkedEntityId(request.getLinkedEntityId())
                .triggeredBy(request.getTriggeredBy())
                .triggerEvent(request.getTriggerEvent())
                .status(Status.PENDING)
                .build();
    }

    private String renderTemplate(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables.get(key);
            String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : "";
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private NotificationSender findSender(Channel channel) {
        return senders.stream()
                .filter(s -> s.getChannel().equals(channel.name()))
                .findFirst()
                .orElse(null);
    }

    private NotificationTemplateDTO toTemplateDTO(NotificationTemplate template) {
        return NotificationTemplateDTO.builder()
                .id(template.getId())
                .code(template.getCode())
                .name(template.getName())
                .description(template.getDescription())
                .channel(template.getChannel())
                .subjectTemplate(template.getSubjectTemplate())
                .bodyTemplate(template.getBodyTemplate())
                .triggerEvent(template.getTriggerEvent())
                .productId(template.getProductId())
                .expectedVariables(template.getExpectedVariables())
                .active(template.getActive())
                .build();
    }

    private NotificationLogDTO toLogDTO(NotificationLog log) {
        return NotificationLogDTO.builder()
                .id(log.getId())
                .templateCode(log.getTemplateCode())
                .channel(log.getChannel())
                .recipient(log.getRecipient())
                .recipientName(log.getRecipientName())
                .subject(log.getSubject())
                .status(log.getStatus())
                .linkedEntityType(log.getLinkedEntityType())
                .linkedEntityId(log.getLinkedEntityId())
                .sentAt(log.getSentAt())
                .createdAt(log.getCreatedAt())
                .errorMessage(log.getErrorMessage())
                .build();
    }
}
