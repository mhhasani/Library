package com.library.labeling;

import com.library.labeling.OutputLabelService.OutputLabel;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

/**
 * Stamps the output label (classification, user, IP, time) as a footer on every page of a
 * PDF being handed out. The label uses a standard PDF font, so it is rendered in ASCII.
 */
@Slf4j
@Component
public class PdfLabelStamper {

    private static final float FONT_SIZE = 7f;
    private static final float MARGIN = 12f;

    /**
     * Loads and labels the PDF. Returns empty when the file cannot be labeled (e.g. it is
     * encrypted or damaged); the caller then serves the original file.
     */
    public Optional<PDDocument> stamp(File pdf, OutputLabel label) {
        PDDocument document = null;
        try {
            document = Loader.loadPDF(new RandomAccessReadBufferedFile(pdf));
            if (document.isEncrypted()) {
                document.close();
                return Optional.empty();
            }
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            String text = toWinAnsi(label.toAsciiLine());
            for (PDPage page : document.getPages()) {
                stampPage(document, page, font, text);
            }
            return Optional.of(document);
        } catch (IOException | RuntimeException e) {
            log.warn("Could not label PDF {}: {}", pdf.getName(), e.getMessage());
            closeQuietly(document);
            return Optional.empty();
        }
    }

    /** Writes the labeled document and releases it. */
    public void write(PDDocument document, OutputStream out) throws IOException {
        try (document) {
            document.save(out);
        }
    }

    private static void stampPage(PDDocument document, PDPage page, PDType1Font font, String text)
            throws IOException {
        PDRectangle box = page.getMediaBox();
        try (PDPageContentStream content = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            content.beginText();
            content.setFont(font, FONT_SIZE);
            content.setNonStrokingColor(0.55f, 0f, 0f);
            content.newLineAtOffset(box.getLowerLeftX() + MARGIN, box.getLowerLeftY() + MARGIN / 2);
            content.showText(text);
            content.endText();
        }
    }

    /** Standard-14 fonts only cover WinAnsi; anything else is replaced. */
    private static String toWinAnsi(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            out.append(c >= 0x20 && c < 0x7F ? c : '?');
        }
        return out.toString();
    }

    private static void closeQuietly(PDDocument document) {
        if (document == null) return;
        try {
            document.close();
        } catch (IOException ignored) {
            // nothing more to release
        }
    }
}
