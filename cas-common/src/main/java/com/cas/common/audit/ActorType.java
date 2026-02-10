package com.cas.common.audit;

/**
 * Type of actor performing the audited action.
 */
public enum ActorType {
    USER, // Human user
    SYSTEM, // Internal system process
    API_CLIENT, // Service-to-service client
    ANONYMOUS // Unauthenticated actor
}
