package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.UserDTO;
import com.library.entity.enums.AccountStatus;
import com.library.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/users")
@Tag(name = "Admin Users", description = "System admin user management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "Get users", description = "Retrieve users (optionally filter by account status)")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getUsers(
            @RequestParam(value = "status", required = false) AccountStatus status
    ) {
        List<UserDTO> users = userService.getUsers(status);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }
}
