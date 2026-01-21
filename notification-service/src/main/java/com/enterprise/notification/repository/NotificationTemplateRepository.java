package com.enterprise.notification.repository;

import com.enterprise.notification.entity.NotificationTemplate;
import com.enterprise.notification.entity.NotificationTemplate.Channel;
import com.enterprise.notification.entity.NotificationTemplate.TriggerEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByCodeAndActiveTrue(String code);

    List<NotificationTemplate> findByChannelAndActiveTrue(Channel channel);

    List<NotificationTemplate> findByTriggerEventAndActiveTrue(TriggerEvent triggerEvent);

    List<NotificationTemplate> findByTriggerEventAndChannelAndActiveTrue(TriggerEvent event, Channel channel);

    List<NotificationTemplate> findByProductIdAndActiveTrue(UUID productId);

    List<NotificationTemplate> findByActiveTrueOrderByNameAsc();

    boolean existsByCode(String code);
}
