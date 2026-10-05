package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.labeling.OutputLabelService;
import com.library.labeling.OutputLabelService.OutputLabel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The label the client prints on every page/report it outputs. */
@RestController
@RequestMapping("/v1/output-label")
@Tag(name = "Output label", description = "Classification label for printed output")
@SecurityRequirement(name = "Bearer Authentication")
public class OutputLabelController {

    private final OutputLabelService labelService;

    public OutputLabelController(OutputLabelService labelService) {
        this.labelService = labelService;
    }

    public record PrintLabel(String classification, String classificationLabel, Long userId,
                             String userEmail, String clientIp, String requestedAt) {
    }

    @GetMapping
    @Operation(summary = "Label for printed output: classification, user, client IP and time")
    public ResponseEntity<ApiResponse<PrintLabel>> current() {
        OutputLabel label = labelService.labelFor(labelService.systemClassification());
        return ResponseEntity.ok(ApiResponse.success("Output label", new PrintLabel(
                label.classification().name(), label.classification().getPersianLabel(),
                label.userId(), label.userEmail(), label.clientIp(), label.requestedAt())));
    }
}
