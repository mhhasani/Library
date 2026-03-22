package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.LibraryDTO;
import com.library.dto.UserDTO;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import com.library.service.LibraryService;
import com.library.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/admin")
@Tag(name = "Admin", description = "System admin management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private LibraryService libraryService;

    @GetMapping("/users")
    @Operation(summary = "Get users", description = "Retrieve users (optionally filter by account status)")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getUsers(
            @RequestParam(value = "status", required = false) AccountStatus status) {
        List<UserDTO> users = userService.getUsers(status);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Update user account status", description = "Suspend or activate a user account")
    public ResponseEntity<ApiResponse<UserDTO>> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        AccountStatus newStatus = AccountStatus.valueOf(body.get("status"));
        UserDTO user = userService.updateUserStatus(userId, newStatus);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", user));
    }

    @PatchMapping("/users/{userId}/role")
    @Operation(summary = "Update user system role", description = "Promote or demote a user's system role")
    public ResponseEntity<ApiResponse<UserDTO>> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        SystemRole newRole = SystemRole.valueOf(body.get("role"));
        UserDTO user = userService.updateUserRole(userId, newRole);
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", user));
    }

    @GetMapping("/libraries")
    @Operation(summary = "Get all libraries", description = "Retrieve all libraries in the system")
    public ResponseEntity<ApiResponse<List<LibraryDTO>>> getAllLibraries() {
        List<LibraryDTO> libraries = libraryService.getAllLibrariesForAdmin();
        return ResponseEntity.ok(ApiResponse.success("Libraries retrieved successfully", libraries));
    }
}
