package com.library.labeling;

import com.library.entity.enums.ClassificationLevel;
import com.library.labeling.OutputLabelService.OutputLabel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Produces the copy of a PDF that is handed to a user: stamped with the output label.
 * The labeled copy is written to a temporary file that is deleted after streaming.
 */
@Slf4j
@Service
public class LabeledDownloadService {

    private final OutputLabelService labelService;
    private final PdfLabelStamper stamper;

    public LabeledDownloadService(OutputLabelService labelService, PdfLabelStamper stamper) {
        this.labelService = labelService;
        this.stamper = stamper;
    }

    public record LabeledFile(Resource resource, OutputLabel label) {
    }

    public LabeledFile labelPdf(Resource original, ClassificationLevel classification) {
        OutputLabel label = labelService.labelFor(classification);
        if (!original.isFile()) {
            return new LabeledFile(original, label);
        }
        try {
            var stamped = stamper.stamp(original.getFile(), label);
            if (stamped.isEmpty()) {
                return new LabeledFile(original, label);
            }
            Path tmp = Files.createTempFile("labeled-", ".pdf");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                stamper.write(stamped.get(), out);
            }
            return new LabeledFile(new TemporaryFileResource(tmp), label);
        } catch (IOException e) {
            log.warn("Serving unlabeled PDF, labeling failed: {}", e.getMessage());
            return new LabeledFile(original, label);
        }
    }
}
