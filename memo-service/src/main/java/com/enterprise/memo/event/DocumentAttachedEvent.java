package com.enterprise.memo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Spring event published when a document is attached to or detached from a
 * memo.
 */
@Getter
public class DocumentAttachedEvent extends ApplicationEvent {

    private final UUID memoId;
    private final String memoNumber;
    private final UUID documentId;
    private final String fileName;
    private final UUID userId;
    private final String userName;
    private final Action action;

    public enum Action {
        UPLOAD,
        DELETE
    }

    public DocumentAttachedEvent(Object source, UUID memoId, String memoNumber,
            UUID documentId, String fileName,
            UUID userId, String userName, Action action) {
        super(source);
        this.memoId = memoId;
        this.memoNumber = memoNumber;
        this.documentId = documentId;
        this.fileName = fileName;
        this.userId = userId;
        this.userName = userName;
        this.action = action;
    }
}
