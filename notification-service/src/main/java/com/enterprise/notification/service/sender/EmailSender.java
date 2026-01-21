package com.enterprise.notification.service.sender;

import com.enterprise.notification.entity.NotificationLog;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Email sender implementation using Spring Mail.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSender implements NotificationSender {

    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:true}")
    private boolean enabled;

    @Value("${notification.email.from:noreply@example.com}")
    private String fromAddress;

    @Value("${notification.email.from-name:LMS System}")
    private String fromName;

    @Override
    public void send(NotificationLog notification) {
        if (!enabled) {
            log.warn("Email sending is disabled, skipping: {}", notification.getId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(notification.getRecipient());
            helper.setSubject(notification.getSubject() != null ? notification.getSubject() : "Notification");

            // Use HTML body if available, otherwise plain text
            String body = notification.getBody();
            boolean isHtml = body != null && (body.contains("<html") || body.contains("<HTML")
                    || body.contains("<div") || body.contains("<p>"));
            helper.setText(body, isHtml);

            mailSender.send(message);

            log.info("Email sent successfully to: {}", notification.getRecipient());

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", notification.getRecipient(), e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", notification.getRecipient(), e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getChannel() {
        return "EMAIL";
    }
}
