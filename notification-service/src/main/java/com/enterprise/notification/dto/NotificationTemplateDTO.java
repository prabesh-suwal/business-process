package com.enterprise.notification.dto;

import com.enterprise.notification.entity.NotificationTemplate.Channel;
import com.enterprise.notification.entity.NotificationTemplate.TriggerEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateDTO {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private Channel channel;
    private String subjectTemplate;
    private String bodyTemplate;
    private TriggerEvent triggerEvent;
    private UUID productId;
    private Map<String, String> expectedVariables;
    private Boolean active;
}
