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

    @Test
    public void testCreateText_withValidPDF() throws IOException {
        // Create a sample PDF document
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        // Add content to the PDF page
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        String longText = "This is a sample text that is intentionally made long enough to exceed 200 characters. "
                + "It includes multiple sentences to ensure that the content length requirement is met. "
                + "This text will be added to the PDF document for testing purposes.";
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
        contentStream.newLineAtOffset(25, 750);
        contentStream.showText(longText);
        contentStream.endText();
        contentStream.close();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.save(out);
        document.close();

        FileEntity fileEntity = new FileEntity();
        fileEntity.setTitle("Valid PDF");
        fileEntity.setData(out.toByteArray());

        List<PDFText> pdfTexts = iTextProcessorService.createText(fileEntity);

        assertEquals(1, pdfTexts.size());
        assertTrue(pdfTexts.get(0).getText()
                .contains("This is a sample text that is intentionally made long enough to exceed 200 characters."));
        logger.info("testCreateText_withValidPDF passed");
    }

    @Test
    public void testCreateText_withEmptyPDF() throws IOException {
        // Create an empty PDF document
        PDDocument document = new PDDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.save(out);
        document.close();

        FileEntity fileEntity = new FileEntity();
        fileEntity.setTitle("Empty PDF");
        fileEntity.setData(out.toByteArray());

        List<PDFText> pdfTexts = iTextProcessorService.createText(fileEntity);

        assertTrue(pdfTexts.isEmpty());
        logger.info("testCreateText_withEmptyPDF passed");
    }

    @Test
    public void testCreateText_withInvalidPDF() {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setTitle("Invalid PDF");
        fileEntity.setData("Invalid PDF content".getBytes());

        assertThrows(IllegalArgumentException.class, () -> iTextProcessorService.createText(fileEntity));

        logger.info("testCreateText_withInvalidPDF passed");
    }
}