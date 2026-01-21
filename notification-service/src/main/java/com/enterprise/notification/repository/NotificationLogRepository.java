package com.enterprise.notification.repository;

import com.enterprise.notification.entity.NotificationLog;
import com.enterprise.notification.entity.NotificationLog.Status;
import com.enterprise.notification.entity.NotificationTemplate.Channel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    Page<NotificationLog> findByRecipientOrderByCreatedAtDesc(String recipient, Pageable pageable);

    List<NotificationLog> findByLinkedEntityTypeAndLinkedEntityId(String entityType, UUID entityId);

    List<NotificationLog> findByStatusAndRetryCountLessThan(Status status, int maxRetries);

    Page<NotificationLog> findByStatusOrderByCreatedAtDesc(Status status, Pageable pageable);

    Page<NotificationLog> findByChannelOrderByCreatedAtDesc(Channel channel, Pageable pageable);

    List<NotificationLog> findByStatusAndCreatedAtBefore(Status status, LocalDateTime before);

    long countByStatusAndCreatedAtAfter(Status status, LocalDateTime after);
}
