package com.Orio.gbp_tool.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.Orio.gbp_tool.model.FileEntity;
import com.Orio.gbp_tool.model.PDFText;

@SpringBootTest
public class TextProcessorServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(TextProcessorServiceTest.class);

    @Autowired
    private ITextProcessorService iTextProcessorService;

    // Constants
    private static final String VALID_PDF_TITLE = "Valid PDF";
    private static final String EMPTY_PDF_TITLE = "Empty PDF";
    private static final String INVALID_PDF_TITLE = "Invalid PDF";
    private static final String VALID_PDF_TEXT = "This is a sample text that is intentionally made long enough to exceed 200 characters. "
            + "It includes multiple sentences to ensure that the content length requirement is met. "
            + "This text will be added to the PDF document for testing purposes.";
    private static final float TEXT_FONT_SIZE = 12f;
    private static final float TEXT_START_X = 25f;
    private static final float TEXT_START_Y = 750f;

    @Test
    public void testCreateText_withValidPDF() throws IOException {
        logger.info("Starting testCreateText_withValidPDF");

        // Arrange: Create a valid PDF with long text
        byte[] pdfData = createPdfWithText(VALID_PDF_TEXT);
        FileEntity fileEntity = createFileEntity(VALID_PDF_TITLE, pdfData);

        // Act: Process the PDF to extract text
        List<PDFText> pdfTexts = iTextProcessorService.createText(fileEntity);

        // Assert: Verify the extracted text
        assertEquals(1, pdfTexts.size(), "There should be exactly one PDFText entry.");
        assertTrue(
                pdfTexts.get(0).getText().contains(
                        "This is a sample text that is intentionally made long enough to exceed 200 characters."),
                "Extracted text should contain the expected content.");
        logger.info("testCreateText_withValidPDF passed");
    }

    @Test
    public void testCreateText_withEmptyPDF() throws IOException {
        logger.info("Starting testCreateText_withEmptyPDF");

        // Arrange: Create an empty PDF
        byte[] pdfData = createEmptyPdf();
        FileEntity fileEntity = createFileEntity(EMPTY_PDF_TITLE, pdfData);

        // Act: Process the empty PDF to extract text
        List<PDFText> pdfTexts = iTextProcessorService.createText(fileEntity);

        // Assert: Verify that no text is extracted
        assertTrue(pdfTexts.isEmpty(), "Extracted text list should be empty for an empty PDF.");
        logger.info("testCreateText_withEmptyPDF passed");
    }

    @Test
    public void testCreateText_withInvalidPDF() {
        logger.info("Starting testCreateText_withInvalidPDF");

        // Arrange: Create a FileEntity with invalid PDF data
        FileEntity fileEntity = createFileEntity(INVALID_PDF_TITLE, "Invalid PDF content".getBytes());

        // Act & Assert: Expect an IllegalArgumentException when processing invalid PDF
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            iTextProcessorService.createText(fileEntity);
        }, "Processing an invalid PDF should throw IllegalArgumentException.");

        logger.info("testCreateText_withInvalidPDF passed with exception: {}", exception.getMessage());
    }

    // Private Helper Methods

    /**
     * Creates a FileEntity with the given title and data.
     *
     * @param title The title of the file.
     * @param data  The byte data of the file.
     * @return A FileEntity instance.
     */
    private FileEntity createFileEntity(String title, byte[] data) {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setTitle(title);
        fileEntity.setData(data);
        return fileEntity;
    }

    /**
     * Creates a PDF document with the specified text.
     *
     * @param text The text to be added to the PDF.
     * @return A byte array representing the PDF document.
     * @throws IOException If an I/O error occurs.
     */
    private byte[] createPdfWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            // Add content to the PDF page
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TEXT_FONT_SIZE);
                contentStream.newLineAtOffset(TEXT_START_X, TEXT_START_Y);
                contentStream.showText(text);
                contentStream.endText();
            }

            // Save the document to a byte array
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                document.save(out);
                return out.toByteArray();
            }
        }
    }

    /**
     * Creates an empty PDF document.
     *
     * @return A byte array representing an empty PDF document.
     * @throws IOException If an I/O error occurs.
     */
    private byte[] createEmptyPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            // Create an empty PDF without adding any pages
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                document.save(out);
                return out.toByteArray();
            }
        }
    }
}
