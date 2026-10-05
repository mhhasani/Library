package com.library.controller;

import com.library.audit.AuditEntry;
import com.library.audit.AuditQueryService;
import com.library.audit.AuditService;
import com.library.audit.AuditService.ChainVerification;
import com.library.dto.ApiResponse;
import com.library.dto.AuditLogDTO;
import com.library.entity.enums.AuditAction;
import com.library.entity.enums.AuditOutcome;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** Security audit reports for system administrators. */
@RestController
@RequestMapping("/v1/admin/audit-logs")
@Tag(name = "Audit", description = "Security audit trail reports")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminAuditController {

    private final AuditQueryService queryService;
    private final AuditService auditService;

    public AdminAuditController(AuditQueryService queryService, AuditService auditService) {
        this.queryService = queryService;
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Search the audit trail", description = "Filter by event, outcome, free text (user/IP/details) and time range")
    public ResponseEntity<ApiResponse<Page<AuditLogDTO>>> search(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var filter = new AuditQueryService.Filter(action, outcome, search, from, to);
        var result = queryService.search(filter, PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(ApiResponse.success("Audit records retrieved", result));
    }

    @GetMapping("/actions")
    @Operation(summary = "Audited event types")
    public ResponseEntity<ApiResponse<List<String>>> actions() {
        return ResponseEntity.ok(ApiResponse.success("Audit actions",
                Arrays.stream(AuditAction.values()).map(Enum::name).toList()));
    }

    @GetMapping("/export")
    @Operation(summary = "Export the filtered audit trail as labeled CSV")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        var filter = new AuditQueryService.Filter(action, outcome, search, from, to);
        byte[] csv = queryService.exportCsv(filter);
        auditService.record(AuditEntry.success(AuditAction.AUDIT_LOG_EXPORT).entityType("AUDIT_LOG").build());
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("audit-log.csv").build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(csv);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify the integrity of the audit hash chain")
    public ResponseEntity<ApiResponse<ChainVerification>> verify() {
        ChainVerification result = auditService.verifyChain();
        auditService.record((result.valid() ? AuditEntry.success(AuditAction.AUDIT_CHAIN_VERIFY)
                        : AuditEntry.failure(AuditAction.AUDIT_CHAIN_VERIFY))
                .entityType("AUDIT_LOG").details("checked=" + result.recordsChecked()
                        + (result.valid() ? "" : " firstInvalidId=" + result.firstInvalidId()))
                .build());
        return ResponseEntity.ok(ApiResponse.success("Audit chain verified", result));
    }
}
