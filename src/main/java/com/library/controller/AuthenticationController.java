package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.SessionInfoDTO;
import com.library.security.AuthSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session endpoints for the SPA. Login, registration and password changes happen in
 * Keycloak (start at /api/oauth2/authorization/keycloak); logout is POST /api/v1/auth/logout.
 */
@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "Session state and post-login security notice")
public class AuthenticationController {

    private final AuthSessionService authSessionService;

    public AuthenticationController(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @GetMapping("/session")
    @Operation(summary = "Current session", description = "Who is logged in and what the security notice must show")
    public ResponseEntity<ApiResponse<SessionInfoDTO>> session(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Session", authSessionService.describe(request)));
    }

    @PostMapping("/notice")
    @Operation(summary = "Acknowledge the post-login security notice")
    public ResponseEntity<ApiResponse<Void>> acknowledgeNotice(HttpServletRequest request) {
        authSessionService.acknowledgeNotice(request);
        return ResponseEntity.ok(ApiResponse.success("Notice acknowledged"));
    }
}
