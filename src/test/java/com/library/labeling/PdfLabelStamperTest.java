package com.library.labeling;

import com.library.entity.enums.ClassificationLevel;
import com.library.labeling.OutputLabelService.OutputLabel;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PDF output labeling")
class PdfLabelStamperTest {

    private final PdfLabelStamper stamper = new PdfLabelStamper();
    private final OutputLabel label = new OutputLabel(
            ClassificationLevel.CONFIDENTIAL, 42L, "reader@x.ir", "10.0.0.7", "2026-10-05 12:00:00");

    @Test
    @DisplayName("Every page carries the classification, user, IP and time")
    void everyPageIsLabeled(@TempDir Path dir) throws Exception {
        Path pdf = dir.resolve("book.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.addPage(new PDPage());
            doc.save(pdf.toFile());
        }

        PDDocument stamped = stamper.stamp(pdf.toFile(), label).orElseThrow();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        stamper.write(stamped, out);

        try (PDDocument result = Loader.loadPDF(out.toByteArray())) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= 2; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                assertThat(stripper.getText(result))
                        .contains("Classification: CONFIDENTIAL")
                        .contains("reader@x.ir")
                        .contains("10.0.0.7")
                        .contains("2026-10-05 12:00:00");
            }
        }
    }

    @Test
    @DisplayName("A file that is not a valid PDF is left untouched (caller serves the original)")
    void invalidPdfIsNotLabeled(@TempDir Path dir) throws Exception {
        Path notPdf = Files.writeString(dir.resolve("x.pdf"), "not a pdf");
        assertThat(stamper.stamp(notPdf.toFile(), label)).isEmpty();
    }
}
