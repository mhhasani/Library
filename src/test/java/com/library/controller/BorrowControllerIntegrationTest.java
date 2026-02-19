package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.BorrowRequest;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.Borrow;
import com.library.entity.DigitalBook;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.User;
import com.library.entity.enums.*;
import com.library.repository.BookCopyRepository;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRepository;
import com.library.repository.DigitalBookRepository;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
public class BorrowControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private DigitalBookRepository digitalBookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryMembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User memberUser;
    private Library testLibrary;
    private Book physicalBook;
    private BookCopy bookCopy;
    private Book digitalBook;

    @BeforeEach
    void setUp() {
        borrowRepository.deleteAll();
        digitalBookRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        membershipRepository.deleteAll();
        libraryRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = User.builder()
                .email("admin@example.com")
                .passwordHash("hashedpassword")
                .firstName("Admin")
                .lastName("User")
                .systemRole(SystemRole.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        adminUser = userRepository.save(adminUser);

        memberUser = User.builder()
                .email("member@example.com")
                .passwordHash("hashedpassword")
                .firstName("Member")
                .lastName("User")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        memberUser = userRepository.save(memberUser);

        testLibrary = Library.builder()
                .name("Test Library")
                .description("A test library")
                .ownerId(adminUser.getId())
                .location("Test City")
                .phone("+1234567890")
                .email("library@example.com")
                .isActive(true)
                .autoMembershipApproval(true)
                .defaultBorrowDurationDays(14)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testLibrary = libraryRepository.save(testLibrary);

        LibraryMembership adminMembership = LibraryMembership.builder()
                .libraryId(testLibrary.getId())
                .userId(adminUser.getId())
                .role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .approvedBy(adminUser.getId())
                .build();
        membershipRepository.save(adminMembership);

        LibraryMembership memberMembership = LibraryMembership.builder()
                .libraryId(testLibrary.getId())
                .userId(memberUser.getId())
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .approvedBy(adminUser.getId())
                .build();
        membershipRepository.save(memberMembership);

        physicalBook = Book.builder()
                .libraryId(testLibrary.getId())
                .title("Physical Book")
                .author("Test Author")
                .isbn("111-222-333")
                .description("A physical book")
                .publishedYear(2023)
                .publisher("Test Publisher")
                .category("Fiction")
                .language("English")
                .isActive(true)
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        physicalBook = bookRepository.save(physicalBook);

        bookCopy = BookCopy.builder()
                .bookId(physicalBook.getId())
                .copyNumber(1)
                .status(BookCopyStatus.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        bookCopy = bookCopyRepository.save(bookCopy);

        digitalBook = Book.builder()
                .libraryId(testLibrary.getId())
                .title("Digital Book")
                .author("Digital Author")
                .isbn("444-555-666")
                .description("A digital book")
                .publishedYear(2023)
                .publisher("Digital Publisher")
                .category("Science")
                .language("English")
                .isActive(true)
                .autoDigitalBorrowEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        digitalBook = bookRepository.save(digitalBook);

        DigitalBook digitalBookFile = DigitalBook.builder()
                .bookId(digitalBook.getId())
                .format(BookFormat.PDF)
                .filePath("/files/book.pdf")
                .fileSize(1024000L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        digitalBookRepository.save(digitalBookFile);
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = {"USER"})
    void testCreatePhysicalBorrowRequest() throws Exception {
        BorrowRequest request = BorrowRequest.builder()
                .bookId(physicalBook.getId())
                .borrowType(BorrowType.PHYSICAL)
                .build();

        mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/borrows/" + physicalBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", equalTo("REQUESTED")))
                .andExpect(jsonPath("$.data.borrowType", equalTo("PHYSICAL")));
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = {"USER"})
    void testCreateDigitalBorrowRequest() throws Exception {
        BorrowRequest request = BorrowRequest.builder()
                .bookId(digitalBook.getId())
                .borrowType(BorrowType.DIGITAL)
                .build();

        mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/borrows/" + digitalBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", equalTo("APPROVED")))
                .andExpect(jsonPath("$.data.borrowType", equalTo("DIGITAL")));
    }

    @Test
    void testGetUserBorrows() throws Exception {
        // First create a borrow request
        BorrowRequest request = BorrowRequest.builder()
                .bookId(digitalBook.getId())
                .borrowType(BorrowType.DIGITAL)
                .build();

        var response = mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/borrows/" + digitalBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(request1 -> {
                            request1.setUserPrincipal(new org.springframework.security.core.userdetails.User(
                                    "member@example.com", "",
                                    java.util.Collections.singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                            ));
                            return request1;
                        }))
                .andExpect(status().isCreated())
                .andReturn();

        // Now get user borrows
        mockMvc.perform(get("/v1/libraries/" + testLibrary.getId() + "/borrows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request1 -> {
                            request1.setUserPrincipal(new org.springframework.security.core.userdetails.User(
                                    "member@example.com", "",
                                    java.util.Collections.singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                            ));
                            return request1;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testGetPendingBorrows() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + testLibrary.getId() + "/borrows/pending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", isA(java.util.ArrayList.class)));
    }

    @Test
    void testApproveBorrowRequest() throws Exception {
        // Create a borrow request first
        BorrowRequest request = BorrowRequest.builder()
                .bookId(physicalBook.getId())
                .borrowType(BorrowType.PHYSICAL)
                .build();

        var response = mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/borrows/" + physicalBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(request1 -> {
                            request1.setUserPrincipal(new org.springframework.security.core.userdetails.User(
                                    "member@example.com", "",
                                    java.util.Collections.singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                            ));
                            return request1;
                        }))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        Long borrowId = objectMapper.readTree(responseBody)
                .get("data")
                .get("id")
                .asLong();

        // Approve the borrow
        mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/borrows/" + borrowId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", equalTo("APPROVED")))
                .andExpect(jsonPath("$.data.approvedAt", notNullValue()));
    }

    @Test
    void testRejectBorrowRequest() throws Exception {
        // Create a borrow request first
        BorrowRequest request = BorrowRequest.builder()
                .bookId(physicalBook.getId())
                .borrowType(BorrowType.PHYSICAL)
                .build();

        var response = mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/borrows/" + physicalBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(request1 -> {
                            request1.setUserPrincipal(new org.springframework.security.core.userdetails.User(
                                    "member@example.com", "",
                                    java.util.Collections.singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                            ));
                            return request1;
                        }))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        Long borrowId = objectMapper.readTree(responseBody)
                .get("data")
                .get("id")
                .asLong();

        // Reject the borrow
        mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/borrows/" + borrowId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("reason", "Book is reserved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", equalTo("REJECTED")))
                .andExpect(jsonPath("$.data.rejectionReason", equalTo("Book is reserved")));
    }

    @Test
    void testReturnBook() throws Exception {
        // Create and approve a borrow
        BorrowRequest request = BorrowRequest.builder()
                .bookId(physicalBook.getId())
                .borrowType(BorrowType.PHYSICAL)
                .build();

        var createResponse = mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/borrows/" + physicalBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(request1 -> {
                            request1.setUserPrincipal(new org.springframework.security.core.userdetails.User(
                                    "member@example.com", "",
                                    java.util.Collections.singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                            ));
                            return request1;
                        }))
                .andExpect(status().isCreated())
                .andReturn();

        String createBody = createResponse.getResponse().getContentAsString();
        Long borrowId = objectMapper.readTree(createBody)
                .get("data")
                .get("id")
                .asLong();

        // Approve
        mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/borrows/" + borrowId + "/approve")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Return
        mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/borrows/" + borrowId + "/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request1 -> {
                            request1.setUserPrincipal(new org.springframework.security.core.userdetails.User(
                                    "member@example.com", "",
                                    java.util.Collections.singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                            ));
                            return request1;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", equalTo("RETURNED")))
                .andExpect(jsonPath("$.data.returnDate", notNullValue()));
    }
}
