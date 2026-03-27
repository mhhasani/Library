package com.library.service;

import com.library.dto.LibrarySubjectDTO;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.LibrarySubject;
import com.library.entity.enums.LibraryMembershipRole;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.LibrarySubjectRepository;
import com.library.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class LibrarySubjectService {

    @Autowired
    private LibrarySubjectRepository subjectRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryMembershipRepository membershipRepository;

    public List<LibrarySubjectDTO> getSubjects(Long libraryId) {
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found"));
        return subjectRepository.findByLibraryIdOrderByNameAsc(libraryId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public LibrarySubjectDTO createSubject(Long libraryId, String name) {
        requireAdmin(libraryId);
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found"));

        if (name == null || name.isBlank()) {
            throw new BadRequestException("Subject name cannot be empty");
        }
        name = name.trim();

        if (subjectRepository.existsByLibraryIdAndName(libraryId, name)) {
            throw new BadRequestException("Subject already exists: " + name);
        }

        LibrarySubject subject = LibrarySubject.builder()
                .library(library)
                .name(name)
                .build();
        subject = subjectRepository.save(subject);
        log.info("Subject '{}' created for library {}", name, libraryId);
        return toDTO(subject);
    }

    public void deleteSubject(Long libraryId, Long subjectId) {
        requireAdmin(libraryId);
        LibrarySubject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (!subject.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("Subject does not belong to this library");
        }
        subjectRepository.delete(subject);
        log.info("Subject {} deleted from library {}", subjectId, libraryId);
    }

    private void requireAdmin(Long libraryId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("Not a member of this library"));
        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can manage subjects");
        }
    }

    public LibrarySubjectDTO toDTO(LibrarySubject s) {
        return LibrarySubjectDTO.builder()
                .id(s.getId())
                .libraryId(s.getLibrary().getId())
                .name(s.getName())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
