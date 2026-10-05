package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * State of the caller's session for the SPA: who is logged in and what the post-login
 * security notice must show (previous login, failed attempts since then).
 */
@Schema(description = "Current session")
public record SessionInfoDTO(
        boolean authenticated,
        UserDTO user,
        boolean noticeAcknowledged,
        String organizationName,
        String currentLoginIp,
        String previousLoginAt,
        String previousLoginIp,
        Integer failedLoginsSinceLastLogin,
        Integer idleTimeoutMinutes) {

    public static SessionInfoDTO anonymous() {
        return new SessionInfoDTO(false, null, false, null, null, null, null, null, null);
    }
}
