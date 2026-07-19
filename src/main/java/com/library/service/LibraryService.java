package com.library.service;

import com.library.dto.LibraryDTO;
import com.library.dto.LibraryRequest;
import com.library.dto.MembershipDTO;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.User;
import com.library.entity.enums.LibraryMembershipRole;
import com.library.entity.enums.MembershipStatus;
import com.library.entity.enums.NotificationType;
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

    @Autowired
    private NotificationService notificationService;

    private static final String ENTITY_MEMBERSHIP = "LIBRARY_MEMBERSHIP";

    private String fullName(User u) {
        String name = ((u.getFirstName() != null ? u.getFirstName() : "") + " " +
                (u.getLastName() != null ? u.getLastName() : "")).trim();
        return name.isEmpty() ? u.getEmail() : name;
    }

    /** Notify every ADMIN member of a library. */
    private void notifyLibraryAdmins(Library library, NotificationType type, String title, String message, Long relatedId) {
        membershipRepository.findByLibraryId(library.getId()).stream()
                .filter(m -> m.getRole() == LibraryMembershipRole.ADMIN)
                .forEach(m -> notificationService.notify(m.getUser(), type, title, message,
                        ENTITY_MEMBERSHIP, relatedId, "/libraries/" + library.getId() + "/admin/members"));
    }

    public LibraryDTO createLibrary(LibraryRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        // A system admin may assign a different user as the owner/admin of the new library
        Long ownerId = (request.getOwnerUserId() != null && SecurityUtils.hasRole("SYSTEM_ADMIN"))
                ? request.getOwnerUserId() : currentUserId;
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("کاربر مالک پیدا نشد"));

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
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));

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

    /** Paginated + searchable libraries for the system-admin table (search by name/description/owner). */
    public org.springframework.data.domain.Page<LibraryDTO> getAllLibrariesForAdminPaged(
            String search, org.springframework.data.domain.Pageable pageable) {
        final String q = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        org.springframework.data.jpa.domain.Specification<Library> spec = (root, cq, cb) -> {
            if (q == null) return cb.conjunction();
            String pat = "%" + q + "%";
            var owner = root.join("owner");
            return cb.or(
                cb.like(cb.lower(root.get("name")), pat),
                cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pat),
                cb.like(cb.lower(cb.coalesce(owner.get("firstName"), "")), pat),
                cb.like(cb.lower(cb.coalesce(owner.get("lastName"), "")), pat),
                cb.like(cb.lower(owner.get("email")), pat)
            );
        };
        return libraryRepository.findAll(spec, pageable).map(lib -> mapToLibraryDTO(lib, null));
    }

    public LibraryDTO updateLibrary(Long libraryId, LibraryRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));

        // A library ADMIN, the owner, or a system admin may edit the library
        boolean isSystemAdmin = SecurityUtils.hasRole("SYSTEM_ADMIN");
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElse(null);
        boolean isLibraryAdmin = membership != null && membership.getRole() == LibraryMembershipRole.ADMIN;
        if (!isSystemAdmin && !isLibraryAdmin) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند اطلاعات کتابخانه را ویرایش کند");
        }

        library.setName(request.getName());
        library.setDescription(request.getDescription());
        library.setAutoMembershipApproval(request.getAutoMembershipApproval());
        library.setDefaultBorrowDurationDays(request.getDefaultBorrowDurationDays());
        if (request.getIsActive() != null) {
            library.setIsActive(request.getIsActive());
        }
        library.setUpdatedAt(LocalDateTime.now());

        library = libraryRepository.save(library);
        log.info("Library updated: {}", library.getName());

        return mapToLibraryDTO(library, membership);
    }

    /**
     * Promote a member to ADMIN or demote an ADMIN to MEMBER.
     * Allowed only for the library owner or a system admin. The owner's role cannot be changed.
     */
    public LibraryDTO setMemberRole(Long libraryId, Long targetUserId, LibraryMembershipRole newRole) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));

        boolean isOwner = library.getOwner().getId().equals(currentUserId);
        boolean isSystemAdmin = SecurityUtils.hasRole("SYSTEM_ADMIN");
        if (!isOwner && !isSystemAdmin) {
            throw new UnauthorizedException("فقط مالک کتابخانه یا مدیر سیستم می‌تواند نقش اعضا را تغییر دهد");
        }
        if (library.getOwner().getId().equals(targetUserId)) {
            throw new BadRequestException("نقش مالک کتابخانه قابل تغییر نیست");
        }

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(targetUserId, libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("این کاربر عضو این کتابخانه نیست"));
        if (membership.getStatus() != MembershipStatus.APPROVED) {
            throw new BadRequestException("فقط اعضای تأییدشده را می‌توان ارتقا/تنزل داد");
        }

        membership.setRole(newRole);
        membership.setUpdatedAt(LocalDateTime.now());
        membershipRepository.save(membership);
        log.info("Member {} role set to {} in library {}", targetUserId, newRole, libraryId);

        String roleFa = newRole == LibraryMembershipRole.ADMIN ? "مدیر کتابخانه" : "عضو عادی";
        notificationService.notify(membership.getUser(), NotificationType.LIBRARY_ROLE_CHANGED,
                "نقش شما تغییر کرد",
                String.format("نقش شما در کتابخانه «%s» به «%s» تغییر یافت.", library.getName(), roleFa),
                ENTITY_MEMBERSHIP, membership.getId(), "/libraries/" + libraryId + "/books");

        return mapToLibraryDTO(library, null);
    }

    public void deleteLibrary(Long libraryId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));

        if (!library.getOwner().getId().equals(currentUserId) && !SecurityUtils.hasRole("SYSTEM_ADMIN")) {
            throw new UnauthorizedException("فقط مالک کتابخانه می‌تواند کتابخانه را حذف کند");
        }

        library.setIsActive(false);
        library.setUpdatedAt(LocalDateTime.now());
        libraryRepository.save(library);
        log.info("Library deleted: {}", library.getName());
    }

    public void requestMembership(Long libraryId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("کاربر فعلی پیدا نشد"));

        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));

        // Check if membership already exists
        if (membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId).isPresent()) {
            throw new BadRequestException("شما از قبل عضو هستید یا درخواست در انتظار دارید");
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

        if (membership.getStatus() == MembershipStatus.APPROVED) {
            // Auto-approved by the library's settings — tell the user right away.
            notificationService.notify(user, NotificationType.MEMBERSHIP_APPROVED,
                    "عضویت تأیید شد",
                    String.format("عضویت شما در کتابخانه «%s» به‌صورت خودکار تأیید شد.", library.getName()),
                    ENTITY_MEMBERSHIP, membership.getId(), "/libraries/" + libraryId + "/books");
        } else {
            notifyLibraryAdmins(library, NotificationType.NEW_MEMBERSHIP_REQUEST,
                    "درخواست عضویت جدید",
                    String.format("کاربر %s درخواست عضویت در کتابخانه «%s» را ثبت کرد.", fullName(user), library.getName()),
                    membership.getId());
        }
    }

    public void approveMembership(Long libraryId, Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        // Check if current user is library admin
        LibraryMembership adminMembership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));

        if (adminMembership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند عضویت‌ها را تأیید کند");
        }

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(userId, libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("عضویت پیدا نشد"));

        membership.setStatus(MembershipStatus.APPROVED);
        membership.setApprovedBy(userRepository.findById(currentUserId).orElseThrow());
        membership.setUpdatedAt(LocalDateTime.now());
        membershipRepository.save(membership);
        log.info("Membership approved for user {} in library {}", userId, libraryId);

        notificationService.notify(membership.getUser(), NotificationType.MEMBERSHIP_APPROVED,
                "عضویت تأیید شد",
                String.format("عضویت شما در کتابخانه «%s» تأیید شد.", membership.getLibrary().getName()),
                ENTITY_MEMBERSHIP, membership.getId(), "/libraries/" + libraryId + "/books");
    }

    public void rejectMembership(Long libraryId, Long userId, String rejectionReason) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        LibraryMembership adminMembership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));

        if (adminMembership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند عضویت‌ها را رد کند");
        }

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(userId, libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("عضویت پیدا نشد"));

        membership.setStatus(MembershipStatus.REJECTED);
        membership.setRejectionReason(rejectionReason);
        membership.setUpdatedAt(LocalDateTime.now());
        membershipRepository.save(membership);
        log.info("Membership rejected for user {} in library {}", userId, libraryId);

        notificationService.notify(membership.getUser(), NotificationType.MEMBERSHIP_REJECTED,
                "عضویت رد شد",
                String.format("درخواست عضویت شما در کتابخانه «%s» رد شد.%s", membership.getLibrary().getName(),
                        rejectionReason != null && !rejectionReason.isBlank() ? " دلیل: " + rejectionReason : ""),
                ENTITY_MEMBERSHIP, membership.getId(), "/libraries");
    }

    public List<MembershipDTO> getLibraryMembers(Long libraryId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LibraryMembership adminMembership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));
        if (adminMembership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند فهرست اعضا را ببیند");
        }
        return membershipRepository.findByLibraryId(libraryId).stream()
                .map(this::mapToMembershipDTO)
                .collect(Collectors.toList());
    }

    private void requireLibraryAdmin(Long libraryId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LibraryMembership m = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));
        if (m.getRole() != LibraryMembershipRole.ADMIN && !SecurityUtils.hasRole("SYSTEM_ADMIN")) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند فهرست اعضا را ببیند");
        }
    }

    /** Pending membership requests (small list, no pagination). */
    public List<MembershipDTO> getPendingMembers(Long libraryId) {
        requireLibraryAdmin(libraryId);
        return membershipRepository.findByLibraryIdAndStatus(libraryId, MembershipStatus.PENDING).stream()
                .map(this::mapToMembershipDTO)
                .collect(Collectors.toList());
    }

    /** Paginated + searchable non-pending members (search by name/email/role). */
    public org.springframework.data.domain.Page<MembershipDTO> getMembersPaged(
            Long libraryId, String search, org.springframework.data.domain.Pageable pageable) {
        requireLibraryAdmin(libraryId);
        final String q = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        org.springframework.data.jpa.domain.Specification<LibraryMembership> spec = (root, cq, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> ps = new java.util.ArrayList<>();
            ps.add(cb.equal(root.get("library").get("id"), libraryId));
            ps.add(cb.notEqual(root.get("status"), MembershipStatus.PENDING));
            if (q != null) {
                var user = root.join("user");
                String pat = "%" + q + "%";
                ps.add(cb.or(
                    cb.like(cb.lower(user.get("email")), pat),
                    cb.like(cb.lower(cb.coalesce(user.get("firstName"), "")), pat),
                    cb.like(cb.lower(cb.coalesce(user.get("lastName"), "")), pat),
                    cb.like(cb.lower(user.get("phoneNumber")), pat)
                ));
            }
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return membershipRepository.findAll(spec, pageable).map(this::mapToMembershipDTO);
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
