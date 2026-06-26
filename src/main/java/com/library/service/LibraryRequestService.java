package com.library.service;

import com.library.dto.LibraryCreationRequestDTO;
import com.library.dto.LibraryRequest;
import com.library.entity.Library;
import com.library.entity.LibraryCreationRequest;
import com.library.entity.LibraryMembership;
import com.library.entity.User;
import com.library.entity.enums.LibraryMembershipRole;
import com.library.entity.enums.LibraryRequestStatus;
import com.library.entity.enums.MembershipStatus;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.LibraryCreationRequestRepository;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.UserRepository;
import com.library.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class LibraryRequestService {

    @Autowired private LibraryCreationRequestRepository requestRepository;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private LibraryMembershipRepository membershipRepository;
    @Autowired private UserRepository userRepository;

    private void requireSystemAdmin() {
        if (!SecurityUtils.hasRole("SYSTEM_ADMIN")) {
            throw new UnauthorizedException("فقط مدیر سیستم می‌تواند این کار را انجام دهد");
        }
    }

    public LibraryCreationRequestDTO createRequest(LibraryRequest req) {
        Long uid = SecurityUtils.getCurrentUserId();
        User requester = userRepository.findById(uid)
                .orElseThrow(() -> new UnauthorizedException("کاربر فعلی پیدا نشد"));
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BadRequestException("نام کتابخانه را وارد کنید");
        }
        LibraryCreationRequest entity = LibraryCreationRequest.builder()
                .requester(requester)
                .name(req.getName())
                .description(req.getDescription())
                .autoMembershipApproval(Boolean.TRUE.equals(req.getAutoMembershipApproval()))
                .defaultBorrowDurationDays(req.getDefaultBorrowDurationDays() != null ? req.getDefaultBorrowDurationDays() : 14)
                .status(LibraryRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        entity = requestRepository.save(entity);
        log.info("Library creation request {} submitted by {}", entity.getId(), requester.getEmail());
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<LibraryCreationRequestDTO> getMyRequests() {
        Long uid = SecurityUtils.getCurrentUserId();
        return requestRepository.findByRequesterIdOrderByCreatedAtDesc(uid).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LibraryCreationRequestDTO> getAllRequests(LibraryRequestStatus status) {
        requireSystemAdmin();
        List<LibraryCreationRequest> list = status != null
                ? requestRepository.findByStatusOrderByCreatedAtDesc(status)
                : requestRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    /** Approve a request (optionally editing fields) → creates the library with the requester as owner. */
    public LibraryCreationRequestDTO approveRequest(Long requestId, LibraryRequest override) {
        requireSystemAdmin();
        Long adminId = SecurityUtils.getCurrentUserId();
        User admin = userRepository.findById(adminId).orElseThrow();

        LibraryCreationRequest reqEntity = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("درخواست پیدا نشد"));
        if (reqEntity.getStatus() != LibraryRequestStatus.PENDING) {
            throw new BadRequestException("فقط درخواست‌های در انتظار قابل تأیید هستند");
        }

        // Apply optional edits from the reviewer
        if (override != null) {
            if (override.getName() != null && !override.getName().isBlank()) reqEntity.setName(override.getName());
            if (override.getDescription() != null) reqEntity.setDescription(override.getDescription());
            if (override.getAutoMembershipApproval() != null) reqEntity.setAutoMembershipApproval(override.getAutoMembershipApproval());
            if (override.getDefaultBorrowDurationDays() != null) reqEntity.setDefaultBorrowDurationDays(override.getDefaultBorrowDurationDays());
        }

        User owner = reqEntity.getRequester();
        Library library = Library.builder()
                .name(reqEntity.getName())
                .description(reqEntity.getDescription())
                .owner(owner)
                .autoMembershipApproval(reqEntity.getAutoMembershipApproval())
                .defaultBorrowDurationDays(reqEntity.getDefaultBorrowDurationDays())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        library = libraryRepository.save(library);

        membershipRepository.save(LibraryMembership.builder()
                .user(owner).library(library)
                .role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .approvedBy(admin)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        reqEntity.setStatus(LibraryRequestStatus.APPROVED);
        reqEntity.setReviewedBy(admin);
        reqEntity.setCreatedLibrary(library);
        reqEntity.setUpdatedAt(LocalDateTime.now());
        reqEntity = requestRepository.save(reqEntity);
        log.info("Library request {} approved → library {} (owner {})", requestId, library.getId(), owner.getEmail());
        return toDto(reqEntity);
    }

    public LibraryCreationRequestDTO rejectRequest(Long requestId, String reason) {
        requireSystemAdmin();
        Long adminId = SecurityUtils.getCurrentUserId();
        User admin = userRepository.findById(adminId).orElseThrow();
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("ذکر دلیل رد الزامی است");
        }
        LibraryCreationRequest reqEntity = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("درخواست پیدا نشد"));
        if (reqEntity.getStatus() != LibraryRequestStatus.PENDING) {
            throw new BadRequestException("فقط درخواست‌های در انتظار قابل رد هستند");
        }
        reqEntity.setStatus(LibraryRequestStatus.REJECTED);
        reqEntity.setRejectionReason(reason);
        reqEntity.setReviewedBy(admin);
        reqEntity.setUpdatedAt(LocalDateTime.now());
        reqEntity = requestRepository.save(reqEntity);
        log.info("Library request {} rejected", requestId);
        return toDto(reqEntity);
    }

    private LibraryCreationRequestDTO toDto(LibraryCreationRequest e) {
        User r = e.getRequester();
        String name = r != null ? ((r.getFirstName() != null ? r.getFirstName() : "") + " " +
                (r.getLastName() != null ? r.getLastName() : "")).trim() : "";
        return LibraryCreationRequestDTO.builder()
                .id(e.getId())
                .requesterId(r != null ? r.getId() : null)
                .requesterName(name.isEmpty() && r != null ? r.getEmail() : name)
                .requesterEmail(r != null ? r.getEmail() : null)
                .name(e.getName())
                .description(e.getDescription())
                .autoMembershipApproval(e.getAutoMembershipApproval())
                .defaultBorrowDurationDays(e.getDefaultBorrowDurationDays())
                .status(e.getStatus())
                .rejectionReason(e.getRejectionReason())
                .createdLibraryId(e.getCreatedLibrary() != null ? e.getCreatedLibrary().getId() : null)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
