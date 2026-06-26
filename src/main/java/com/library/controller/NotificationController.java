package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.NotificationDTO;
import com.library.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/notifications")
@Tag(name = "Notifications", description = "In-app notifications")
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List my notifications", description = "Returns the current user's notifications, newest first")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notificationService.getMyNotifications()));
    }

    @GetMapping("/paged")
    @Operation(summary = "List my notifications (paginated)",
            description = "Paginated notifications for infinite scroll, newest first")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<NotificationDTO>>> listPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved",
                notificationService.getMyNotificationsPaged(pageable)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread notification count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved",
                Map.of("count", notificationService.getUnreadCount())));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }
}
