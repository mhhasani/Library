package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.BorrowDTO;
import com.library.dto.BorrowRequest;
import com.library.entity.enums.BorrowStatus;
import com.library.entity.enums.BorrowType;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.service.BorrowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Borrow Controller Tests")
class BorrowControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BorrowService borrowService;

    private BorrowRequest borrowRequest;
    private BorrowDTO borrowDTO;

    @BeforeEach
    void setUp() {
        borrowRequest = BorrowRequest.builder()
                .borrowType(BorrowType.PHYSICAL)
                .bookCopyId(1L)
                .build();

        borrowDTO = BorrowDTO.builder()
                .id(1L)
                .bookId(1L)
                .userId(1L)
                .borrowType(BorrowType.PHYSICAL)
                .bookCopyId(1L)
                .build();
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should create physical borrow request successfully")
    void testCreateBorrowPhysicalSuccess() throws Exception {
        when(borrowService.createBorrowRequest(1L, 1L, borrowRequest)).thenReturn(borrowDTO);

        mockMvc.perform(post("/v1/libraries/1/borrows/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(borrowRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Borrow request created successfully"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should create digital borrow request successfully")
    void testCreateBorrowDigitalSuccess() throws Exception {
        BorrowRequest digitalRequest = BorrowRequest.builder()
                .borrowType(BorrowType.DIGITAL)
                .build();

        BorrowDTO digitalBorrow = BorrowDTO.builder()
                .id(2L)
                .bookId(1L)
                .userId(1L)
                .borrowType(BorrowType.DIGITAL)
                .build();

        when(borrowService.createBorrowRequest(1L, 1L, digitalRequest)).thenReturn(digitalBorrow);

        mockMvc.perform(post("/v1/libraries/1/borrows/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(digitalRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.borrowType").value("DIGITAL"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 404 when book not found")
    void testCreateBorrowBookNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Book not found"))
                .when(borrowService).createBorrowRequest(1L, 999L, borrowRequest);

        mockMvc.perform(post("/v1/libraries/1/borrows/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(borrowRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 400 when book copy not available")
    void testCreateBorrowBookCopyNotAvailable() throws Exception {
        doThrow(new BadRequestException("Book copy is not available"))
                .when(borrowService).createBorrowRequest(1L, 1L, borrowRequest);

        mockMvc.perform(post("/v1/libraries/1/borrows/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(borrowRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 401 when user not member")
    void testCreateBorrowNotMember() throws Exception {
        doThrow(new UnauthorizedException("User is not a member of this library"))
                .when(borrowService).createBorrowRequest(1L, 1L, borrowRequest);

        mockMvc.perform(post("/v1/libraries/1/borrows/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(borrowRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should approve borrow request successfully")
    void testApproveBorrowSuccess() throws Exception {
        BorrowDTO approvedBorrow = BorrowDTO.builder()
                .id(1L)
                .bookId(1L)
                .userId(1L)
                .status(BorrowStatus.APPROVED)
                .build();

        when(borrowService.approveBorrowRequest(1L, 1L)).thenReturn(approvedBorrow);

        mockMvc.perform(post("/v1/libraries/1/borrows/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should reject borrow request successfully")
    void testRejectBorrowSuccess() throws Exception {
        BorrowDTO rejectedBorrow = BorrowDTO.builder()
                .id(1L)
                .bookId(1L)
                .userId(1L)
                .status(BorrowStatus.REJECTED)
                .rejectionReason("Book no longer available")
                .build();

        when(borrowService.rejectBorrowRequest(1L, 1L, "Book no longer available"))
                .thenReturn(rejectedBorrow);

        mockMvc.perform(post("/v1/libraries/1/borrows/1/reject")
                .param("reason", "Book no longer available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void testCreateBorrowUnAuthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/borrows/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(borrowRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should get all library borrows for admin successfully")
    void testGetLibraryAdminBorrowsSuccess() throws Exception {
        BorrowDTO activeBorrow = BorrowDTO.builder()
                .id(2L)
                .bookId(1L)
                .userId(3L)
                .userEmail("member@test.com")
                .bookTitle("Clean Code")
                .borrowType(BorrowType.PHYSICAL)
                .status(BorrowStatus.APPROVED)
                .build();

        when(borrowService.getLibraryBorrows(1L, null, null)).thenReturn(java.util.Arrays.asList(activeBorrow));

        mockMvc.perform(get("/v1/libraries/1/borrows/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userEmail").value("member@test.com"))
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should get filtered library borrows by status")
    void testGetLibraryAdminBorrowsFilteredByStatus() throws Exception {
        when(borrowService.getLibraryBorrows(1L, BorrowStatus.APPROVED, null))
                .thenReturn(java.util.Arrays.asList(borrowDTO));

        mockMvc.perform(get("/v1/libraries/1/borrows/admin/all")
                .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "member@library.com", roles = "USER")
    @DisplayName("Should return 401 when non-admin requests all borrows")
    void testGetLibraryAdminBorrowsUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can view all borrows"))
                .when(borrowService).getLibraryBorrows(1L, null, null);

        mockMvc.perform(get("/v1/libraries/1/borrows/admin/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should create physical borrow request successfully without bookCopyId")
    void testCreateBorrowPhysicalSuccessWithoutCopyId() throws Exception {
        BorrowRequest requestWithoutCopyId = BorrowRequest.builder()
                .borrowType(BorrowType.PHYSICAL)
                .build();

        BorrowDTO autoPicked = BorrowDTO.builder()
                .id(1L)
                .bookId(1L)
                .userId(1L)
                .borrowType(BorrowType.PHYSICAL)
                .bookCopyId(1L)
                .build();

        when(borrowService.createBorrowRequest(1L, 1L, requestWithoutCopyId)).thenReturn(autoPicked);

        mockMvc.perform(post("/v1/libraries/1/borrows/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithoutCopyId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Borrow request created successfully"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 400 when user already has an active physical borrow of this book")
    void testCreateBorrowDuplicateRejected() throws Exception {
        BorrowRequest requestWithoutCopyId = BorrowRequest.builder()
                .borrowType(BorrowType.PHYSICAL)
                .build();

        doThrow(new BadRequestException("User already has an active physical borrow of this book"))
                .when(borrowService).createBorrowRequest(1L, 1L, requestWithoutCopyId);

        mockMvc.perform(post("/v1/libraries/1/borrows/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithoutCopyId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 400 when no available copy of this book")
    void testCreateBorrowNoAvailableCopy() throws Exception {
        BorrowRequest requestWithoutCopyId = BorrowRequest.builder()
                .borrowType(BorrowType.PHYSICAL)
                .build();

        doThrow(new BadRequestException("No available copy of this book"))
                .when(borrowService).createBorrowRequest(1L, 1L, requestWithoutCopyId);

        mockMvc.perform(post("/v1/libraries/1/borrows/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithoutCopyId)))
                .andExpect(status().isBadRequest());
    }
}
