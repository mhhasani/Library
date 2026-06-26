package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.BookDTO;
import com.library.entity.Book;
import com.library.entity.Library;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import com.library.exception.ResourceNotFoundException;
import com.library.repository.BookRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.UserRepository;
import com.library.util.SecurityTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Favorite Service Tests")
class FavoriteServiceTest extends BaseIntegrationTest {

    @Autowired private FavoriteService favoriteService;
    @Autowired private UserRepository userRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private LibraryRepository libraryRepository;

    private User user;
    private Book book;
    private Book book2;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("fav-user@lib.com").passwordHash("$2a$10$x")
                .firstName("F").lastName("U").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        User owner = userRepository.save(User.builder()
                .email("fav-owner@lib.com").passwordHash("$2a$10$x")
                .firstName("O").lastName("W").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        Library lib = libraryRepository.save(Library.builder()
                .name("Fav Library").owner(owner).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        book = bookRepository.save(Book.builder()
                .title("Clean Code").author("Martin").library(lib)
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        book2 = bookRepository.save(Book.builder()
                .title("DDD").author("Evans").library(lib)
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(user, "USER");
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    @Test
    @DisplayName("toggle() adds favorite when not yet favorited — returns true")
    void toggle_adds_returnsTrue() {
        boolean result = favoriteService.toggle(book.getId());
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("toggle() removes favorite when already favorited — returns false")
    void toggle_removes_returnsFalse() {
        favoriteService.toggle(book.getId()); // add
        boolean result = favoriteService.toggle(book.getId()); // remove
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("toggle() throws when book not found")
    void toggle_bookNotFound_throws() {
        assertThatThrownBy(() -> favoriteService.toggle(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getFavoriteBookIds() returns IDs of favorited books")
    void getFavoriteBookIds_returnsList() {
        favoriteService.toggle(book.getId());
        favoriteService.toggle(book2.getId());

        List<Long> ids = favoriteService.getFavoriteBookIds();
        assertThat(ids).containsExactlyInAnyOrder(book.getId(), book2.getId());
    }

    @Test
    @DisplayName("getFavoriteBookIds() returns empty list when no favorites")
    void getFavoriteBookIds_empty() {
        assertThat(favoriteService.getFavoriteBookIds()).isEmpty();
    }

    @Test
    @DisplayName("getFavorites() returns paginated favorites")
    void getFavorites_paginated() {
        favoriteService.toggle(book.getId());
        favoriteService.toggle(book2.getId());

        Page<BookDTO> result = favoriteService.getFavorites(null, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("getFavorites() filters by search query")
    void getFavorites_search_filtered() {
        favoriteService.toggle(book.getId());
        favoriteService.toggle(book2.getId());

        Page<BookDTO> result = favoriteService.getFavorites("clean", PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("getFavorites() returns empty when search yields no matches")
    void getFavorites_searchNoMatch_empty() {
        favoriteService.toggle(book.getId());

        Page<BookDTO> result = favoriteService.getFavorites("xyz", PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}
