package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.AuthResponse;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.dto.UserDTO;
import com.library.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input or email already exists"
            )
    })
    public ResponseEntity<ApiResponse<UserDTO>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering user: {}", request.getEmail());
        UserDTO user = authenticationService.register(request);
        return new ResponseEntity<>(
                ApiResponse.success("User registered successfully", user),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate user and get JWT tokens")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials"
            )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());
        AuthResponse response = authenticationService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get a new access token using refresh token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired refresh token"
            )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);  // Remove "Bearer " prefix
        log.info("Refreshing access token");
        AuthResponse response = authenticationService.refreshToken(token);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }
}
