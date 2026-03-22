package com.library.service;

import com.library.dto.UserDTO;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.repository.UserRepository;
import com.library.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<UserDTO> getUsers(AccountStatus status) {
        List<User> users = status == null
                ? userRepository.findAll()
                : userRepository.findByAccountStatus(status);

        return users.stream().map(this::toDto).collect(Collectors.toList());
    }

    public UserDTO updateUserStatus(Long userId, AccountStatus newStatus) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(userId)) {
            throw new BadRequestException("Cannot change your own account status");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setAccountStatus(newStatus);
        user.setUpdatedAt(LocalDateTime.now());
        return toDto(userRepository.save(user));
    }

    public UserDTO updateUserRole(Long userId, SystemRole newRole) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(userId)) {
            throw new BadRequestException("Cannot change your own system role");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setSystemRole(newRole);
        user.setUpdatedAt(LocalDateTime.now());
        return toDto(userRepository.save(user));
    }

    private UserDTO toDto(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .systemRole(user.getSystemRole())
                .accountStatus(user.getAccountStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
