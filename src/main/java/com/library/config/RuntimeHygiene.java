package com.library.config;

import com.library.logging.LogMarkers;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * survive the process. Also records application start/stop in the audit log stream.
 */
@Slf4j
@Component
public class RuntimeHygiene {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final Path uploadTmpDir;

    public RuntimeHygiene(@Value("${spring.servlet.multipart.location:}") String uploadTmpDir) {
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
        AUDIT.info("event=APPLICATION_START outcome=SUCCESS");
    }

    @EventListener(ContextClosedEvent.class)
    public void onStop() {
        cleanUploadTmpDir();
        AUDIT.info("event=APPLICATION_STOP outcome=SUCCESS");
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
