package com.library.service;

import com.library.audit.AuditEntry;
import com.library.audit.AuditService;
import com.library.dto.ChangePasswordRequest;
import com.library.dto.UpdateProfileRequest;
import com.library.dto.UserDTO;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.AuditAction;
import com.library.entity.enums.ClassificationLevel;
import com.library.entity.enums.NotificationType;
import com.library.entity.enums.SystemRole;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.repository.UserRepository;
import com.library.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    private static final String ENTITY_USER = "USER";

    private static String statusFa(AccountStatus s) {
        return switch (s) {
            case ACTIVE -> "فعال";
            case SUSPENDED -> "معلق";
            case DELETED -> "حذف‌شده";
            case PENDING_VERIFICATION -> "در انتظار تأیید";
        };
    }

    private static String roleFa(SystemRole r) {
        return switch (r) {
            case SUPER_ADMIN -> "مدیر اصلی";
            case SYSTEM_ADMIN -> "مدیر سیستم";
            case USER -> "کاربر عادی";
        };
    }

    public UserDTO getCurrentUserProfile() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("کاربر پیدا نشد"));
        return toDto(user);
    }

    public UserDTO updateProfile(UpdateProfileRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("کاربر پیدا نشد"));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDeliveryAddress(request.getDeliveryAddress());
        user.setInternalExtension(request.getInternalExtension());
        return toDto(userRepository.save(user));
    }

    public void changePassword(ChangePasswordRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("کاربر پیدا نشد"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("رمز عبور فعلی نادرست است");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public List<UserDTO> getUsers(AccountStatus status) {
        List<User> users = status == null
                ? userRepository.findAll()
                : userRepository.findByAccountStatus(status);

        return users.stream().map(this::toDto).collect(Collectors.toList());
    }

    /** Paginated + searchable users for the system-admin table (search by name/email/phone). */
    public org.springframework.data.domain.Page<UserDTO> getUsersPaged(
            AccountStatus status, String search, org.springframework.data.domain.Pageable pageable) {
        final String q = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        org.springframework.data.jpa.domain.Specification<User> spec = (root, cq, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> ps = new java.util.ArrayList<>();
            if (status != null) ps.add(cb.equal(root.get("accountStatus"), status));
            if (q != null) {
                String pat = "%" + q + "%";
                ps.add(cb.or(
                    cb.like(cb.lower(root.get("email")), pat),
                    cb.like(cb.lower(cb.coalesce(root.get("firstName"), "")), pat),
                    cb.like(cb.lower(cb.coalesce(root.get("lastName"), "")), pat),
                    cb.like(cb.lower(cb.coalesce(root.get("phoneNumber"), "")), pat)
                ));
            }
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return userRepository.findAll(spec, pageable).map(this::toDto);
    }

    public UserDTO updateUserStatus(Long userId, AccountStatus newStatus) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(userId)) {
            throw new BadRequestException("نمی‌توانید وضعیت حساب خودتان را تغییر دهید");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("کاربری با این شناسه پیدا نشد: " + userId));

        if (newStatus != AccountStatus.ACTIVE) {
            List<SystemRole> adminRoles = List.of(SystemRole.SUPER_ADMIN, SystemRole.SYSTEM_ADMIN);
            if (adminRoles.contains(user.getSystemRole())) {
                if (user.getSystemRole() == SystemRole.SUPER_ADMIN) {
                    long activeSuperAdminCount = userRepository.countBySystemRoleAndAccountStatus(
                            SystemRole.SUPER_ADMIN, AccountStatus.ACTIVE);
                    if (activeSuperAdminCount <= 1) {
                        throw new BadRequestException("آخرین ادمین اصلی فعال را نمی‌توان تعلیق کرد");
                    }
                }
                long activeAdminCount = userRepository.countBySystemRoleInAndAccountStatus(
                        adminRoles, AccountStatus.ACTIVE);
                if (activeAdminCount <= 1) {
                    throw new BadRequestException("آخرین مدیر سیستم فعال را نمی‌توان تعلیق یا حذف کرد");
                }
            }
        }

        AccountStatus oldStatus = user.getAccountStatus();
        user.setAccountStatus(newStatus);
        user.setUpdatedAt(LocalDateTime.now());
        UserDTO dto = toDto(userRepository.save(user));

        if (oldStatus != newStatus) {
            auditService.record(AuditEntry.success(AuditAction.USER_STATUS_CHANGE)
                    .entityType(ENTITY_USER).entityId(user.getId())
                    .details(oldStatus + " -> " + newStatus).build());
            notificationService.notify(user, NotificationType.ACCOUNT_STATUS_CHANGED,
                    "وضعیت حساب تغییر کرد",
                    String.format("وضعیت حساب شما به «%s» تغییر یافت.", statusFa(newStatus)),
                    ENTITY_USER, user.getId(), "/profile");
        }
        return dto;
    }

    public UserDTO updateUserRole(Long userId, SystemRole newRole) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(userId)) {
            throw new BadRequestException("نمی‌توانید نقش خودتان را تغییر دهید");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("کاربری با این شناسه پیدا نشد: " + userId));

        List<SystemRole> adminRoles = List.of(SystemRole.SUPER_ADMIN, SystemRole.SYSTEM_ADMIN);

        if (user.getSystemRole() == SystemRole.SUPER_ADMIN && newRole != SystemRole.SUPER_ADMIN) {
            long superAdminCount = userRepository.countBySystemRole(SystemRole.SUPER_ADMIN);
            if (superAdminCount <= 1) {
                throw new BadRequestException("آخرین ادمین اصلی را نمی‌توان تنزل رتبه داد");
            }
        }

        if (adminRoles.contains(user.getSystemRole()) && !adminRoles.contains(newRole)) {
            long adminCount = userRepository.countBySystemRoleIn(adminRoles);
            if (adminCount <= 1) {
                throw new BadRequestException("حداقل یک مدیر سیستم باید وجود داشته باشد");
            }
        }

        SystemRole oldRole = user.getSystemRole();
        user.setSystemRole(newRole);
        user.setUpdatedAt(LocalDateTime.now());
        UserDTO dto = toDto(userRepository.save(user));

        if (oldRole != newRole) {
            auditService.record(AuditEntry.success(AuditAction.USER_ROLE_CHANGE)
                    .entityType(ENTITY_USER).entityId(user.getId())
                    .details(oldRole + " -> " + newRole).build());
            notificationService.notify(user, NotificationType.SYSTEM_ROLE_CHANGED,
                    "نقش سیستمی شما تغییر کرد",
                    String.format("نقش سیستمی شما به «%s» تغییر یافت.", roleFa(newRole)),
                    ENTITY_USER, user.getId(), "/profile");
        }
        return dto;
    }

    /** Grants a classification clearance; recorded as an access-control change. */
    public UserDTO updateUserClearance(Long userId, ClassificationLevel newClearance) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(userId)) {
            throw new BadRequestException("نمی‌توانید سطح دسترسی خودتان را تغییر دهید");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("کاربری با این شناسه پیدا نشد: " + userId));

        ClassificationLevel oldClearance = user.getClearance();
        user.setClearance(newClearance);
        user.setUpdatedAt(LocalDateTime.now());
        UserDTO dto = toDto(userRepository.save(user));

        if (oldClearance != newClearance) {
            auditService.record(AuditEntry.success(AuditAction.USER_CLEARANCE_CHANGE)
                    .entityType(ENTITY_USER).entityId(user.getId())
                    .details(oldClearance + " -> " + newClearance).build());
        }
        return dto;
    }

    private UserDTO toDto(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .deliveryAddress(user.getDeliveryAddress())
                .internalExtension(user.getInternalExtension())
                .systemRole(user.getSystemRole())
                .accountStatus(user.getAccountStatus())
                .clearance(user.getClearance())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
