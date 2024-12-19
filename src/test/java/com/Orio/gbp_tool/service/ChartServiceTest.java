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

    // Constants
    private static final String TEST_FILE_TITLE = "test_file";
    private static final String SAMPLE_PDF_NAME = "sample.pdf";
    private static final String PDF_MIME_TYPE = "application/pdf";
    private static final String CHART_LABEL = "Test Chart";
    private static final String NON_EXISTING_PDF_NAME = "notfound.pdf";

    private static final byte[] VALID_PDF_CONTENT = ("%PDF-1.4\n" +
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
            "<< /Length 145 >>\n" +
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

    @AfterEach
    public void tearDown() {
        removeFileQuietly(TEST_FILE_TITLE);
    }

    // Private Helper Methods

    /**
     * Creates a MockMultipartFile with the given parameters.
     *
     * @param originalFilename The original filename of the file.
     * @param contentType      The MIME type of the file.
     * @param content          The byte content of the file.
     * @return A MockMultipartFile instance.
     */
    private MockMultipartFile createMockPdfFile(String originalFilename, String contentType, byte[] content) {
        return new MockMultipartFile("file", originalFilename, contentType, content);
    }

    /**
     * Saves a test PDF file to the repository.
     *
     * @throws FileDataReadingException          If there is an error reading the
     *                                           file data.
     * @throws FileAlreadyInTheDatabaseException If the file already exists in the
     *                                           database.
     */
    private void saveTestFile() throws FileDataReadingException, FileAlreadyInTheDatabaseException {
        MockMultipartFile mockFile = createMockPdfFile(SAMPLE_PDF_NAME, PDF_MIME_TYPE, VALID_PDF_CONTENT);
        sqlRepo.saveFile(mockFile, TEST_FILE_TITLE);
        logger.info("File '{}' saved successfully.", SAMPLE_PDF_NAME);
    }

    /**
     * Removes a file from the repository quietly, logging the outcome.
     *
     * @param fileTitle The title of the file to remove.
     */
    private void removeFileQuietly(String fileTitle) {
        try {
            sqlRepo.removeFile(fileTitle);
            logger.info("File '{}' removed successfully.", fileTitle);
        } catch (FileNotFoundException e) {
            logger.info("File not found during teardown: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected exception while removing file '{}': {}", fileTitle, e.getMessage());
        }
    }

    /**
     * Creates a list of concepts.
     *
     * @return A list of concept strings.
     */
    private List<String> createConcepts(String... concepts) {
        return Arrays.asList(concepts);
    }

    /**
     * Creates a list of PDF file names.
     *
     * @return A list of PDF file name strings.
     */
    private List<String> createPdfNames(String... pdfNames) {
        return Arrays.asList(pdfNames);
    }

    /**
     * Creates a list with a single non-existing PDF file name.
     *
     * @return A list containing a non-existing PDF file name.
     */
    private List<String> createNonExistingPdfNames() {
        return Arrays.asList(NON_EXISTING_PDF_NAME);
    }

    // Test Methods

    @Test
    public void testCreateChartSuccess()
            throws NoPdfFoundException, FileDataReadingException, FileAlreadyInTheDatabaseException {
        logger.info("Starting testCreateChartSuccess");

        // Arrange: Save a valid PDF file
        saveTestFile();

        // Act: Create a chart with valid inputs
        List<String> concepts = createConcepts("concept1", "concept2");
        List<String> pdfNames = createPdfNames(TEST_FILE_TITLE);
        String label = CHART_LABEL;

        ChartDataResult result = chartService.createChart(concepts, pdfNames, label);
        logger.info("Chart created successfully with label: {}", label);

        // Assert: Validate the results
        assertEquals(label, result.getChartData().getLabel(),
                "The chart label should match the input label.");
        assertEquals(2, result.getChartData().getData().size(),
                "The number of data entries should match the number of concepts.");
        assertEquals(0, result.getPdfsNotFound().size(),
                "There should be no PDFs not found.");
        logger.info("testCreateChartSuccess completed successfully.");
    }

    @Test
    public void testCreateChartNoPdfFound() {
        logger.info("Starting testCreateChartNoPdfFound");

        // Arrange: Define concepts and non-existing PDF names
        List<String> concepts = createConcepts("concept1");
        List<String> pdfNames = createNonExistingPdfNames();
        String label = CHART_LABEL;

        // Act & Assert: Expect NoPdfFoundException
        Exception exception = assertThrows(NoPdfFoundException.class, () -> {
            chartService.createChart(concepts, pdfNames, label);
        }, "Creating a chart with non-existing PDFs should throw NoPdfFoundException.");

        logger.info("testCreateChartNoPdfFound completed successfully with exception: {}", exception.getMessage());
    }
}
