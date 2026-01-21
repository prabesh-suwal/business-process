package com.enterprise.notification.dto;

import com.enterprise.notification.entity.NotificationLog.Status;
import com.enterprise.notification.entity.NotificationTemplate.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogDTO {
    private UUID id;
    private String templateCode;
    private Channel channel;
    private String recipient;
    private String recipientName;
    private String subject;
    private Status status;
    private String linkedEntityType;
    private UUID linkedEntityId;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private String errorMessage;
}
