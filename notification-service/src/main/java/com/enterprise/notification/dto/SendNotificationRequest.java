package com.enterprise.notification.dto;

import com.enterprise.notification.entity.NotificationTemplate.Channel;
import com.enterprise.notification.entity.NotificationTemplate.TriggerEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request to send a notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    // Either templateCode OR inline content
    private String templateCode;

    // Inline content (if no template)
    private String subject;
    private String body;

    // Target
    private Channel channel;
    private String recipient; // Email, phone, userId
    private String recipientName;

    // Template variables
    private Map<String, Object> variables;

    // Linking to business entity
    private String linkedEntityType;
    private UUID linkedEntityId;

    // Who triggered
    private UUID triggeredBy;
    private TriggerEvent triggerEvent;
}
