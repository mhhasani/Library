package com.library.entity.enums;

/** Security-relevant event types recorded in the audit trail. */
public enum AuditAction {
    // Application lifecycle
    APPLICATION_START,
    APPLICATION_STOP,

    // Authentication & session
    LOGIN,
    LOGOUT,
    SESSION_EXPIRED,
    SESSION_TERMINATED,
    REAUTHENTICATION,
    AUTHENTICATION_REQUIRED,

    // Attempts to get around access control
    ACCESS_DENIED,
    CSRF_REJECTED,
    SESSION_BINDING_MISMATCH,

    // Changes to access control and security configuration
    USER_ROLE_CHANGE,
    USER_STATUS_CHANGE,
    USER_CLEARANCE_CHANGE,
    LIBRARY_ROLE_CHANGE,
    MEMBERSHIP_DECISION,
    SECURITY_SETTINGS_CHANGE,
    PASSWORD_RESET_BY_ADMIN,

    // Data-changing API calls (privileged activity trail) and audit access
    API_WRITE,
    AUDIT_LOG_EXPORT,
    AUDIT_CHAIN_VERIFY
}
