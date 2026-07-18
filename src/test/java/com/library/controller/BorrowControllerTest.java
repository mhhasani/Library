package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.BorrowDTO;
import com.library.dto.BorrowEventDTO;
import com.library.dto.BorrowRequest;
import com.library.dto.BorrowerSummaryDTO;
import com.library.dto.DeliveryDetailsRequest;
import com.library.dto.PhysicalApprovalRequest;
import com.library.dto.ReturnRequest;
import com.library.dto.ReturnScheduleRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    // ── Reserve ──────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should reserve a book successfully")
    void testReserveBookSuccess() throws Exception {
        BorrowDTO reservation = BorrowDTO.builder()
                .id(5L).bookId(1L).userId(1L)
                .isReservation(true)
                .status(BorrowStatus.REQUESTED)
                .build();

        when(borrowService.reserveBook(1L, 1L)).thenReturn(reservation);

        mockMvc.perform(post("/v1/libraries/1/borrows/1/reserve"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isReservation").value(true));
    }

    @Test
    @DisplayName("Should return 401 when reserving unauthenticated")
    void testReserveBookUnauthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/borrows/1/reserve"))
                .andExpect(status().isUnauthorized());
    }

    // ── Update physical request (PUT .../request) ───────────────────────────

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should update a pending physical borrow request successfully")
    void testUpdatePhysicalRequestSuccess() throws Exception {
        BorrowRequest updateRequest = BorrowRequest.builder()
                .borrowType(BorrowType.PHYSICAL)
                .deliveryAddress("New address")
                .deliveryExtension("12345678")
                .requestedDurationDays(10)
                .saveToProfile(true)
                .build();

        BorrowDTO updated = BorrowDTO.builder()
                .id(1L).bookId(1L).userId(1L)
                .deliveryAddress("New address")
                .build();

        when(borrowService.updatePhysicalRequest(1L, 1L, updateRequest)).thenReturn(updated);

        mockMvc.perform(put("/v1/libraries/1/borrows/1/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deliveryAddress").value("New address"));
    }

    @Test
    @DisplayName("Should return 401 when updating physical request unauthenticated")
    void testUpdatePhysicalRequestUnauthenticated() throws Exception {
        mockMvc.perform(put("/v1/libraries/1/borrows/1/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(borrowRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ── Approve physical borrow ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should approve physical borrow with delivery details successfully")
    void testApprovePhysicalBorrowSuccess() throws Exception {
        PhysicalApprovalRequest approvalRequest = PhysicalApprovalRequest.builder()
                .plannedDeliveryDate(LocalDateTime.of(2026, 6, 28, 10, 0))
                .approvedDurationDays(5)
                .copyUniqueCode("LIB-A-00123")
                .courierName("علی رضایی")
                .bookCopyId(1L)
                .build();

        BorrowDTO approved = BorrowDTO.builder()
                .id(1L).bookId(1L).userId(1L)
                .status(BorrowStatus.APPROVED)
                .courierName("علی رضایی")
                .copyUniqueCode("LIB-A-00123")
                .approvedDurationDays(5)
                .build();

        when(borrowService.approvePhysicalBorrow(1L, 1L, approvalRequest)).thenReturn(approved);

        mockMvc.perform(post("/v1/libraries/1/borrows/1/approve-physical")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.courierName").value("علی رضایی"));
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should return 400 when approve-physical missing required fields")
    void testApprovePhysicalBorrowValidationFailure() throws Exception {
        PhysicalApprovalRequest invalidRequest = PhysicalApprovalRequest.builder().build();

        mockMvc.perform(post("/v1/libraries/1/borrows/1/approve-physical")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 when approve-physical unauthenticated")
    void testApprovePhysicalBorrowUnauthenticated() throws Exception {
        PhysicalApprovalRequest approvalRequest = PhysicalApprovalRequest.builder()
                .plannedDeliveryDate(LocalDateTime.now())
                .approvedDurationDays(5)
                .build();

        mockMvc.perform(post("/v1/libraries/1/borrows/1/approve-physical")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ── Update delivery details ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should update delivery details successfully")
    void testUpdateDeliveryDetailsSuccess() throws Exception {
        DeliveryDetailsRequest deliveryRequest = DeliveryDetailsRequest.builder()
                .courierName("پست")
                .copyUniqueCode("LIB-B-00042")
                .plannedDeliveryDate(LocalDateTime.of(2026, 6, 28, 10, 0))
                .build();

        BorrowDTO updated = BorrowDTO.builder()
                .id(1L).bookId(1L).userId(1L)
                .courierName("پست")
                .copyUniqueCode("LIB-B-00042")
                .build();

        when(borrowService.updateDeliveryDetails(1L, 1L, deliveryRequest)).thenReturn(updated);

        mockMvc.perform(patch("/v1/libraries/1/borrows/1/delivery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deliveryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courierName").value("پست"));
    }

    @Test
    @DisplayName("Should return 401 when updating delivery details unauthenticated")
    void testUpdateDeliveryDetailsUnauthenticated() throws Exception {
        DeliveryDetailsRequest deliveryRequest = DeliveryDetailsRequest.builder().build();

        mockMvc.perform(patch("/v1/libraries/1/borrows/1/delivery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deliveryRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ── Confirm receipt ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should confirm receipt successfully")
    void testConfirmReceiptSuccess() throws Exception {
        BorrowDTO received = BorrowDTO.builder()
                .id(1L).bookId(1L).userId(1L)
                .receivedAt(LocalDateTime.now())
                .build();

        when(borrowService.confirmReceipt(1L, 1L)).thenReturn(received);

        mockMvc.perform(post("/v1/libraries/1/borrows/1/confirm-receipt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Receipt confirmed successfully"));
    }

    @Test
    @DisplayName("Should return 401 when confirming receipt unauthenticated")
    void testConfirmReceiptUnauthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/borrows/1/confirm-receipt"))
                .andExpect(status().isUnauthorized());
    }

    // ── Request return ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should request return pickup successfully")
    void testRequestReturnSuccess() throws Exception {
        ReturnRequest returnRequest = ReturnRequest.builder()
                .returnAddress("Some address")
                .returnExtension("87654321")
                .preferredDate(LocalDateTime.of(2026, 7, 1, 9, 0))
                .build();

        BorrowDTO borrowWithReturnReq = BorrowDTO.builder()
                .id(1L).bookId(1L).userId(1L)
                .returnAddress("Some address")
                .returnExtension("87654321")
                .returnRequestedAt(LocalDateTime.now())
                .build();

        when(borrowService.requestReturn(1L, 1L, returnRequest)).thenReturn(borrowWithReturnReq);

        mockMvc.perform(post("/v1/libraries/1/borrows/1/request-return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(returnRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.returnAddress").value("Some address"));
    }

    @Test
    @DisplayName("Should return 401 when requesting return unauthenticated")
    void testRequestReturnUnauthenticated() throws Exception {
        ReturnRequest returnRequest = ReturnRequest.builder().build();

        mockMvc.perform(post("/v1/libraries/1/borrows/1/request-return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(returnRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ── Schedule return pickup ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should schedule return pickup successfully")
    void testScheduleReturnPickupSuccess() throws Exception {
        ReturnScheduleRequest scheduleRequest = ReturnScheduleRequest.builder()
                .returnCourierName("پست پیشتاز")
                .returnPlannedDate(LocalDateTime.of(2026, 7, 2, 12, 0))
                .build();

        BorrowDTO scheduled = BorrowDTO.builder()
                .id(1L).bookId(1L).userId(1L)
                .returnCourierName("پست پیشتاز")
                .build();

        when(borrowService.scheduleReturnPickup(1L, 1L, scheduleRequest)).thenReturn(scheduled);

        mockMvc.perform(patch("/v1/libraries/1/borrows/1/return-schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scheduleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.returnCourierName").value("پست پیشتاز"));
    }

    @Test
    @DisplayName("Should return 401 when scheduling return pickup unauthenticated")
    void testScheduleReturnPickupUnauthenticated() throws Exception {
        ReturnScheduleRequest scheduleRequest = ReturnScheduleRequest.builder().build();

        mockMvc.perform(patch("/v1/libraries/1/borrows/1/return-schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scheduleRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ── Cancel return request ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should cancel a pending return request successfully")
    void testCancelReturnRequestSuccess() throws Exception {
        BorrowDTO reverted = BorrowDTO.builder()
                .id(1L).bookId(1L).userId(1L)
                .status(BorrowStatus.APPROVED)
                .build();

        when(borrowService.cancelReturnRequest(1L, 1L)).thenReturn(reverted);

        mockMvc.perform(post("/v1/libraries/1/borrows/1/cancel-return-request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Return request cancelled"));
    }

    @Test
    @DisplayName("Should return 401 when cancelling return request unauthenticated")
    void testCancelReturnRequestUnauthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/borrows/1/cancel-return-request"))
                .andExpect(status().isUnauthorized());
    }

    // ── Confirm handover ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should confirm handover to courier successfully")
    void testConfirmHandoverSuccess() throws Exception {
        BorrowDTO handedOver = BorrowDTO.builder()
                .id(1L).bookId(1L).userId(1L)
                .handedOverByUserAt(LocalDateTime.now())
                .build();

        when(borrowService.confirmHandover(1L, 1L)).thenReturn(handedOver);

        mockMvc.perform(post("/v1/libraries/1/borrows/1/confirm-handover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Handover confirmed successfully"));
    }

    @Test
    @DisplayName("Should return 401 when confirming handover unauthenticated")
    void testConfirmHandoverUnauthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/borrows/1/confirm-handover"))
                .andExpect(status().isUnauthorized());
    }

    // ── Confirm return (librarian) ───────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should confirm physical return successfully")
    void testConfirmReturnSuccess() throws Exception {
        BorrowDTO returned = BorrowDTO.builder()
                .id(1L).bookId(1L).userId(1L)
                .status(BorrowStatus.RETURNED)
                .returnDate(LocalDateTime.now())
                .build();

        when(borrowService.confirmReturnByLibrarian(1L, 1L)).thenReturn(returned);

        mockMvc.perform(post("/v1/libraries/1/borrows/1/confirm-return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETURNED"));
    }

    @Test
    @DisplayName("Should return 401 when confirming return unauthenticated")
    void testConfirmReturnUnauthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/borrows/1/confirm-return"))
                .andExpect(status().isUnauthorized());
    }

    // ── Cancel by user ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should cancel borrow by user successfully")
    void testCancelByUserSuccess() throws Exception {
        BorrowDTO cancelled = BorrowDTO.builder()
                .id(1L).bookId(1L).userId(1L)
                .status(BorrowStatus.CANCELLED)
                .build();

        when(borrowService.cancelByUser(1L, 1L)).thenReturn(cancelled);

        mockMvc.perform(post("/v1/libraries/1/borrows/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Should return 401 when cancelling by user unauthenticated")
    void testCancelByUserUnauthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/borrows/1/cancel"))
                .andExpect(status().isUnauthorized());
    }

    // ── Cancel by librarian ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should cancel borrow by librarian successfully")
    void testCancelByLibrarianSuccess() throws Exception {
        BorrowDTO cancelled = BorrowDTO.builder()
                .id(1L).bookId(1L).userId(1L)
                .status(BorrowStatus.CANCELLED)
                .build();

        when(borrowService.cancelByLibrarian(1L, 1L)).thenReturn(cancelled);

        mockMvc.perform(post("/v1/libraries/1/borrows/1/cancel-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Should return 401 when cancelling by librarian unauthenticated")
    void testCancelByLibrarianUnauthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/borrows/1/cancel-admin"))
                .andExpect(status().isUnauthorized());
    }

    // ── Borrow events timeline ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should get borrow event timeline successfully")
    void testGetBorrowEventsSuccess() throws Exception {
        BorrowEventDTO event = BorrowEventDTO.builder()
                .id(1L)
                .title("Borrow requested")
                .detail("User requested to borrow the book")
                .actorName("user@library.com")
                .createdAt(LocalDateTime.now())
                .build();

        when(borrowService.getBorrowEvents(1L, 1L)).thenReturn(List.of(event));

        mockMvc.perform(get("/v1/libraries/1/borrows/1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Borrow requested"))
                .andExpect(jsonPath("$.data[0].actorName").value("user@library.com"));
    }

    @Test
    @DisplayName("Should return 401 when getting borrow events unauthenticated")
    void testGetBorrowEventsUnauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/1/borrows/1/events"))
                .andExpect(status().isUnauthorized());
    }

    // ── Borrower summary ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should get borrower summary successfully")
    void testGetBorrowerSummarySuccess() throws Exception {
        BorrowerSummaryDTO summary = BorrowerSummaryDTO.builder()
                .userId(1L)
                .userFullName("Jane Doe")
                .userEmail("jane@library.com")
                .userPhone("12345678")
                .activePhysical(1)
                .activeDigital(2)
                .pending(1)
                .overdue(0)
                .returned(3)
                .rejected(1)
                .cancelled(0)
                .totalBorrows(8)
                .activeLoans(List.of(borrowDTO))
                .build();

        when(borrowService.getBorrowerSummary(1L, 1L)).thenReturn(summary);

        mockMvc.perform(get("/v1/libraries/1/borrows/user/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userFullName").value("Jane Doe"))
                .andExpect(jsonPath("$.data.totalBorrows").value(8))
                .andExpect(jsonPath("$.data.activeLoans[0].id").value(1));
    }

    @Test
    @DisplayName("Should return 401 when getting borrower summary unauthenticated")
    void testGetBorrowerSummaryUnauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/1/borrows/user/1/summary"))
                .andExpect(status().isUnauthorized());
    }

    // ── Return book ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return book successfully")
    void testReturnBookSuccess() throws Exception {
        BorrowDTO returned = BorrowDTO.builder()
                .id(1L).bookId(1L).userId(1L)
                .status(BorrowStatus.RETURNED)
                .build();

        when(borrowService.returnBook(1L, 1L)).thenReturn(returned);

        mockMvc.perform(post("/v1/libraries/1/borrows/1/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETURNED"));
    }

    @Test
    @DisplayName("Should return 401 when returning book unauthenticated")
    void testReturnBookUnauthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/borrows/1/return"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 404 when returning a non-existent borrow")
    void testReturnBookNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Borrow not found"))
                .when(borrowService).returnBook(1L, 999L);

        mockMvc.perform(post("/v1/libraries/1/borrows/999/return"))
                .andExpect(status().isNotFound());
    }

    // ── Get user borrows ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should get current user's borrows successfully")
    void testGetUserBorrowsSuccess() throws Exception {
        when(borrowService.getUserBorrows(1L, null, null)).thenReturn(List.of(borrowDTO));

        mockMvc.perform(get("/v1/libraries/1/borrows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should get current user's borrows filtered by status and type")
    void testGetUserBorrowsFiltered() throws Exception {
        when(borrowService.getUserBorrows(1L, BorrowStatus.APPROVED, BorrowType.PHYSICAL))
                .thenReturn(List.of(borrowDTO));

        mockMvc.perform(get("/v1/libraries/1/borrows")
                .param("status", "APPROVED")
                .param("type", "PHYSICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should return 401 when getting user borrows unauthenticated")
    void testGetUserBorrowsUnauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/1/borrows"))
                .andExpect(status().isUnauthorized());
    }

    // ── Pending borrows ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should get pending borrow requests successfully")
    void testGetPendingBorrowsSuccess() throws Exception {
        when(borrowService.getPendingBorrows(1L, null)).thenReturn(List.of(borrowDTO));

        mockMvc.perform(get("/v1/libraries/1/borrows/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("Should return 401 when getting pending borrows unauthenticated")
    void testGetPendingBorrowsUnauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/1/borrows/pending"))
                .andExpect(status().isUnauthorized());
    }

    // ── Admin search (paginated) ─────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should search/paginate library borrows successfully")
    void testSearchLibraryBorrowsSuccess() throws Exception {
        Page<BorrowDTO> page = new PageImpl<>(List.of(borrowDTO), PageRequest.of(0, 15), 1);

        when(borrowService.getLibraryBorrowsPaged(
                eq(1L), isNull(), isNull(), isNull(), eq(false), eq(false), any()))
                .thenReturn(page);

        mockMvc.perform(get("/v1/libraries/1/borrows/admin/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should search/paginate library borrows with filters successfully")
    void testSearchLibraryBorrowsWithFiltersSuccess() throws Exception {
        Page<BorrowDTO> page = new PageImpl<>(List.of(borrowDTO), PageRequest.of(0, 5), 1);

        when(borrowService.getLibraryBorrowsPaged(
                eq(1L), any(), eq(BorrowType.PHYSICAL), eq("clean code"), eq(true), eq(true), any()))
                .thenReturn(page);

        mockMvc.perform(get("/v1/libraries/1/borrows/admin/search")
                .param("statuses", "APPROVED", "REQUESTED")
                .param("type", "PHYSICAL")
                .param("search", "clean code")
                .param("needsAttention", "true")
                .param("overdue", "true")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should return 401 when searching library borrows unauthenticated")
    void testSearchLibraryBorrowsUnauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/1/borrows/admin/search"))
                .andExpect(status().isUnauthorized());
    }
}
