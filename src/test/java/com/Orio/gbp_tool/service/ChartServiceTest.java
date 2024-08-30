package com.Orio.gbp_tool.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.Orio.gbp_tool.exception.FileAlreadyInTheDatabaseException;
import com.Orio.gbp_tool.exception.FileDataReadingException;
import com.Orio.gbp_tool.exception.NoPdfFoundException;
import com.Orio.gbp_tool.model.ChartDataResult;
import com.Orio.gbp_tool.repository.ISQLRepo;

@SpringBootTest
public class ChartServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(ChartServiceTest.class);

    @Autowired
    private IChartService chartService;
    @Autowired
    private ISQLRepo sqlRepo;

    @AfterEach
    public void tearDown() {
        try {
            sqlRepo.removeFile("test_file");
            logger.info("File 'test_file' removed successfully.");
        } catch (FileNotFoundException e) {
            logger.info("File not found: {}", e.getMessage());
        }
    }

    @Test
    public void testCreateChartSuccess()
            throws NoPdfFoundException, FileDataReadingException, FileAlreadyInTheDatabaseException {
        logger.info("Starting testCreateChartSuccess");

        // Create a more realistic and longer PDF content
        byte[] pdfContent = ("%PDF-1.4\n" +
                "1 0 obj\n" +
                "<< /Type /Catalog /Pages 2 0 R >>\n" +
                "endobj\n" +
                "2 0 obj\n" +
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n" +
                "endobj\n" +
                "3 0 obj\n" +
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>\n" +
                "endobj\n" +
                "4 0 obj\n" +
                "<< /Length 145 >>\n" + // Adjusted length
                "stream\n" +
                "BT /F1 24 Tf 100 700 Td (This is a longer piece of text to ensure that the PDF content is at least 200 characters long. "
                +
                "By extending the text, we can ensure that the generated PDF contains more than just a simple 'Hello, World!' message.) Tj ET\n"
                +
                "endstream\n" +
                "endobj\n" +
                "xref\n" +
                "0 5\n" +
                "0000000000 65535 f \n" +
                "0000000010 00000 n \n" +
                "0000000066 00000 n \n" +
                "0000000111 00000 n \n" +
                "0000000217 00000 n \n" +
                "trailer\n" +
                "<< /Size 5 /Root 1 0 R >>\n" +
                "startxref\n" +
                "298\n" +
                "%%EOF\n").getBytes();

        sqlRepo.saveFile(new MockMultipartFile("sample.pdf", "sample.pdf", "application/pdf", pdfContent),
                "test_file");
        logger.info("File 'sample.pdf' saved successfully.");

        List<String> concepts = Arrays.asList("concept1", "concept2");
        List<String> pdfNames = Arrays.asList("test_file");
        String label = "Test Chart";

        ChartDataResult result = chartService.createChart(concepts, pdfNames, label);
        logger.info("Chart created successfully with label: {}", label);

        assertEquals(label, result.getChartData().getLabel());
        assertEquals(2, result.getChartData().getData().size());
        assertEquals(0, result.getPdfsNotFound().size());
        logger.info("testCreateChartSuccess completed successfully.");
    }

    @Test
    public void testCreateChartNoPdfFound() {
        logger.info("Starting testCreateChartNoPdfFound");
        List<String> concepts = Arrays.asList("concept1");
        List<String> pdfNames = Arrays.asList("notfound.pdf");
        String label = "Test Chart";

        assertThrows(NoPdfFoundException.class, () -> {
            chartService.createChart(concepts, pdfNames, label);
        });
        logger.info("testCreateChartNoPdfFound completed successfully.");
    }
}