package com.library.entity.enums;

/**
 * System-level roles (in descending privilege order).
 * Role hierarchy: SUPER_ADMIN > SYSTEM_ADMIN > USER
 */
public enum SystemRole {
    SUPER_ADMIN,   // Full system access + exclusive role management
    SYSTEM_ADMIN,  // Full system access
    USER           // Regular user
}
