package com.library.config;

import com.library.audit.AuditEntry;
import com.library.audit.AuditService;
import com.library.entity.enums.AuditAction;
import com.library.logging.LogMarkers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Execution-environment cleanup: the dedicated upload temp directory is wiped on startup
 * (left-overs from a crash) and again on shutdown, so no temporary copies of uploaded files
 * survive the process. Also records application start/stop in the audit trail.
 */
@Slf4j
@Component
public class RuntimeHygiene {

    private final AuditService auditService;
    private final Path uploadTmpDir;

    public RuntimeHygiene(AuditService auditService,
                          @Value("${spring.servlet.multipart.location:}") String uploadTmpDir) {
        this.auditService = auditService;
        this.uploadTmpDir = uploadTmpDir == null || uploadTmpDir.isBlank() ? null : Paths.get(uploadTmpDir);
        cleanUploadTmpDir();
        if (this.uploadTmpDir != null) {
            try {
                Files.createDirectories(this.uploadTmpDir);
            } catch (IOException e) {
                log.warn("Could not create upload temp dir {}: {}", this.uploadTmpDir, e.getMessage());
            }
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStart() {
        auditService.record(AuditEntry.success(AuditAction.APPLICATION_START).build());
    }

    @EventListener(ContextClosedEvent.class)
    public void onStop() {
        cleanUploadTmpDir();
        auditService.record(AuditEntry.success(AuditAction.APPLICATION_STOP).build());
    }

    @EventListener(ApplicationFailedEvent.class)
    public void onFailure(ApplicationFailedEvent event) {
        log.error(LogMarkers.FATAL, "Application failed to start", event.getException());
    }

    private void cleanUploadTmpDir() {
        if (uploadTmpDir == null || !Files.isDirectory(uploadTmpDir)) return;
        try (Stream<Path> walk = Files.walk(uploadTmpDir)) {
            walk.sorted(Comparator.reverseOrder())
                .filter(p -> !p.equals(uploadTmpDir))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        log.warn("Could not delete temp file {}: {}", p, e.getMessage());
                    }
                });
        } catch (IOException e) {
            log.warn("Could not clean upload temp dir {}: {}", uploadTmpDir, e.getMessage());
        }
    }
}
