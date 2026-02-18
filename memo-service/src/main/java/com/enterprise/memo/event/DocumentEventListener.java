package com.enterprise.memo.event;

import com.cas.common.logging.audit.AuditEventType;
import com.cas.common.logging.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for DocumentAttachedEvent and performs audit logging.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentEventListener {

    private final AuditLogger auditLogger;

    @EventListener
    public void handleDocumentAttached(DocumentAttachedEvent event) {
        String actionVerb = event.getAction() == DocumentAttachedEvent.Action.UPLOAD ? "Uploaded" : "Deleted";
        AuditEventType eventType = event.getAction() == DocumentAttachedEvent.Action.UPLOAD
                ? AuditEventType.CREATE
                : AuditEventType.DELETE;

        log.info("{} attachment '{}' for memo {} (documentId: {})",
                actionVerb, event.getFileName(), event.getMemoNumber(), event.getDocumentId());

        auditLogger.log()
                .eventType(eventType)
                .action(actionVerb + " attachment: " + event.getFileName())
                .module("MEMO")
                .entity("MEMO_ATTACHMENT", event.getDocumentId().toString())
                .businessKey(event.getMemoNumber())
                .newValue(event.getFileName())
                .success();
    }
}
