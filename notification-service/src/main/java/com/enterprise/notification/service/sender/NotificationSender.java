package com.enterprise.notification.service.sender;

import com.enterprise.notification.entity.NotificationLog;

/**
 * Interface for notification senders (Email, SMS, Push).
 */
public interface NotificationSender {

    /**
     * Send the notification.
     */
    void send(NotificationLog notification);

    /**
     * Check if this sender is enabled.
     */
    boolean isEnabled();

    /**
     * Get the channel this sender handles.
     */
    String getChannel();
}
