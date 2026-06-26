package com.library.service;

import com.library.dto.LibrarySubjectDTO;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.LibrarySubject;
import com.library.entity.enums.LibraryMembershipRole;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.BookRepository;
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

    @Autowired
    private BookRepository bookRepository;

    public List<LibrarySubjectDTO> getSubjects(Long libraryId) {
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه پیدا نشد"));
        return subjectRepository.findByLibraryIdOrderByNameAsc(libraryId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public LibrarySubjectDTO createSubject(Long libraryId, String name) {
        requireAdmin(libraryId);
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه پیدا نشد"));

        if (name == null || name.isBlank()) {
            throw new BadRequestException("نام موضوع را وارد کنید");
        }
        name = name.trim();

        if (subjectRepository.existsByLibraryIdAndName(libraryId, name)) {
            throw new BadRequestException("این موضوع از قبل وجود دارد: " + name);
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
                .orElseThrow(() -> new ResourceNotFoundException("موضوع پیدا نشد"));
        if (!subject.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این موضوع مربوط به این کتابخانه نیست");
        }
        // Remove from book_subjects join table first (clears Hibernate L1 cache via clearAutomatically)
        bookRepository.removeSubjectFromAllBooks(subjectId);
        subjectRepository.delete(subject);
        log.info("Subject {} deleted from library {}, removed from all associated books", subjectId, libraryId);
    }

    private void requireAdmin(Long libraryId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));
        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند موضوع‌ها را مدیریت کند");
        }
    }

    public LibrarySubjectDTO toDTO(LibrarySubject s) {
        return LibrarySubjectDTO.builder()
                .id(s.getId())
                .libraryId(s.getLibrary().getId())
                .name(s.getName())
                .bookCount(bookRepository.countBySubjectId(s.getId()))
                .createdAt(s.getCreatedAt())
                .build();
    }
}
