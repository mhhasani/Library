package com.library.service;

import com.library.dto.LibraryDTO;
import com.library.dto.LibraryRequest;
import com.library.dto.MembershipDTO;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.User;
import com.library.entity.enums.LibraryMembershipRole;
import com.library.entity.enums.MembershipStatus;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.UserRepository;
import com.library.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class LibraryService {

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryMembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    public LibraryDTO createLibrary(LibraryRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Current user not found"));

        Library library = Library.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .autoMembershipApproval(request.getAutoMembershipApproval() != null ? request.getAutoMembershipApproval() : false)
                .defaultBorrowDurationDays(request.getDefaultBorrowDurationDays() != null ? request.getDefaultBorrowDurationDays() : 14)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        library = libraryRepository.save(library);

        // Add owner as ADMIN member
        LibraryMembership ownerMembership = LibraryMembership.builder()
                .user(owner)
                .library(library)
                .role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .approvedBy(owner)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        membershipRepository.save(ownerMembership);
        log.info("Library created: {} by user: {}", library.getName(), owner.getEmail());

        return mapToLibraryDTO(library, ownerMembership);
    }

    public LibraryDTO getLibraryById(Long libraryId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found with id: " + libraryId));

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElse(null);

        return mapToLibraryDTO(library, membership);
    }

    public List<LibraryDTO> getUserLibraries() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<LibraryMembership> memberships = membershipRepository.findByUserId(currentUserId);
        return memberships.stream()
                .map(m -> mapToLibraryDTO(m.getLibrary(), m))
                .collect(Collectors.toList());
    }

    public List<LibraryDTO> getAllActiveLibraries() {
        List<Library> libraries = libraryRepository.findByIsActive(true);
        return libraries.stream()
                .map(lib -> mapToLibraryDTO(lib, null))
                .collect(Collectors.toList());
    }

    public List<LibraryDTO> getAllLibrariesForAdmin() {
        return libraryRepository.findAll().stream()
                .map(lib -> mapToLibraryDTO(lib, null))
                .collect(Collectors.toList());
    }

    public LibraryDTO updateLibrary(Long libraryId, LibraryRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found with id: " + libraryId));

        // Check if user is library admin
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this library"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can update library details");
        }

        library.setName(request.getName());
        library.setDescription(request.getDescription());
        library.setAutoMembershipApproval(request.getAutoMembershipApproval());
        library.setDefaultBorrowDurationDays(request.getDefaultBorrowDurationDays());
        library.setUpdatedAt(LocalDateTime.now());

        library = libraryRepository.save(library);
        log.info("Library updated: {}", library.getName());

        return mapToLibraryDTO(library, membership);
    }

    public void deleteLibrary(Long libraryId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found with id: " + libraryId));

        if (!library.getOwner().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only library owner can delete the library");
        }

        library.setIsActive(false);
        library.setUpdatedAt(LocalDateTime.now());
        libraryRepository.save(library);
        log.info("Library deleted: {}", library.getName());
    }

    public void requestMembership(Long libraryId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Current user not found"));

        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found with id: " + libraryId));

        // Check if membership already exists
        if (membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId).isPresent()) {
            throw new BadRequestException("User is already a member or has a pending request");
        }

        LibraryMembership membership = LibraryMembership.builder()
                .user(user)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(library.getAutoMembershipApproval() ? MembershipStatus.APPROVED : MembershipStatus.PENDING)
                .approvedBy(library.getAutoMembershipApproval() ? library.getOwner() : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        membershipRepository.save(membership);
        log.info("Membership requested for library {} by user {}", libraryId, user.getEmail());
    }

    public void approveMembership(Long libraryId, Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        // Check if current user is library admin
        LibraryMembership adminMembership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this library"));

        if (adminMembership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can approve memberships");
        }

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(userId, libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        membership.setStatus(MembershipStatus.APPROVED);
        membership.setApprovedBy(userRepository.findById(currentUserId).orElseThrow());
        membership.setUpdatedAt(LocalDateTime.now());
        membershipRepository.save(membership);
        log.info("Membership approved for user {} in library {}", userId, libraryId);
    }

    public void rejectMembership(Long libraryId, Long userId, String rejectionReason) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        LibraryMembership adminMembership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this library"));

        if (adminMembership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can reject memberships");
        }

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(userId, libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        membership.setStatus(MembershipStatus.REJECTED);
        membership.setRejectionReason(rejectionReason);
        membership.setUpdatedAt(LocalDateTime.now());
        membershipRepository.save(membership);
        log.info("Membership rejected for user {} in library {}", userId, libraryId);
    }

    public List<MembershipDTO> getLibraryMembers(Long libraryId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LibraryMembership adminMembership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this library"));
        if (adminMembership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can view member list");
        }
        return membershipRepository.findByLibraryId(libraryId).stream()
                .map(this::mapToMembershipDTO)
                .collect(Collectors.toList());
    }

    private MembershipDTO mapToMembershipDTO(LibraryMembership m) {
        return MembershipDTO.builder()
                .id(m.getId())
                .userId(m.getUser().getId())
                .userEmail(m.getUser().getEmail())
                .userName(m.getUser().getFirstName() + " " + m.getUser().getLastName())
                .libraryId(m.getLibrary().getId())
                .role(m.getRole())
                .status(m.getStatus())
                .approvedById(m.getApprovedBy() != null ? m.getApprovedBy().getId() : null)
                .rejectionReason(m.getRejectionReason())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private LibraryDTO mapToLibraryDTO(Library library, LibraryMembership membership) {
        return LibraryDTO.builder()
                .id(library.getId())
                .name(library.getName())
                .description(library.getDescription())
                .ownerId(library.getOwner().getId())
                .ownerName(library.getOwner().getFirstName() + " " + library.getOwner().getLastName())
                .autoMembershipApproval(library.getAutoMembershipApproval())
                .defaultBorrowDurationDays(library.getDefaultBorrowDurationDays())
                .isActive(library.getIsActive())
                .userRole(membership != null ? membership.getRole() : null)
                .userStatus(membership != null ? membership.getStatus() : null)
                .createdAt(library.getCreatedAt())
                .updatedAt(library.getUpdatedAt())
                .build();
    }
}
