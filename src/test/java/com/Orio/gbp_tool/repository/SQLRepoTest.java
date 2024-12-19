package com.Orio.gbp_tool.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// Consider importing only necessary annotations if possible
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.Orio.gbp_tool.exception.ChartNotFoundException;
import com.Orio.gbp_tool.exception.FileAlreadyInTheDatabaseException;
import com.Orio.gbp_tool.exception.FileDataReadingException;
import com.Orio.gbp_tool.model.ChartData;
import com.Orio.gbp_tool.repository.impl.sql.ChartRepo;

@SpringBootTest
public class SQLRepoTest {

    private static final Logger logger = LoggerFactory.getLogger(SQLRepoTest.class);

    @Autowired
    private ISQLRepo sqlRepo;

    @Autowired
    private ChartRepo chartRepo;

    // Constants
    private static final String TEST_FILE_TITLE = "Test Title";
    private static final String TEST_CHART_LABEL = "Test Chart";
    private static final String PDF_MIME_TYPE = "application/pdf";
    private static final String TEXT_MIME_TYPE = "text/plain";
    private static final String TEST_PDF_NAME = "test.pdf";
    private static final String TEST_TXT_NAME = "test.txt";
    private static final String TEST_FILE_CONTENT = "test content";
    private static final String IO_EXCEPTION_MESSAGE = "IO Exception";
    private static final String NON_EXISTING_CHART_LABEL = "Non Existing Chart";

    @AfterEach
    public void tearDown() {
        logger.info("Cleaning up after test");
        removeFileQuietly(TEST_FILE_TITLE);
        removeChartQuietly(TEST_CHART_LABEL);
    }

    // Private helper methods

    private MultipartFile createMockFile(String name, String originalFilename, String contentType, byte[] content) {
        return new MockMultipartFile(name, originalFilename, contentType, content);
    }

    private MultipartFile createMockFileThatThrowsIOException() {
        return new MockMultipartFile("file", TEST_PDF_NAME, PDF_MIME_TYPE, new byte[0]) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException(IO_EXCEPTION_MESSAGE);
            }
        };
    }

    private void removeFileQuietly(String fileTitle) {
        try {
            sqlRepo.removeFile(fileTitle);
        } catch (FileNotFoundException e) {
            logger.info(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected exception while removing file: {}", e.getMessage());
        }
    }

    private void removeChartQuietly(String chartLabel) {
        try {
            sqlRepo.removeChart(chartLabel);
        } catch (ChartNotFoundException e) {
            logger.info(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected exception while removing chart: {}", e.getMessage());
        }
    }

    private ChartData createChartData(String label) {
        ChartData chartData = new ChartData();
        chartData.setLabel(label);
        return chartData;
    }

    private void saveChartData(String label) {
        ChartData chartData = createChartData(label);
        sqlRepo.saveChart(chartData);
    }

    private boolean isChartExists(String label) {
        List<ChartData> charts = chartRepo.findAll();
        return charts.stream().anyMatch(chart -> chart.getLabel().equals(label));
    }

    // Test methods

    @Test
    public void testSaveFile_withValidPDF()
            throws IOException, FileDataReadingException, FileAlreadyInTheDatabaseException, Exception {
        MultipartFile file = createMockFile("file", TEST_PDF_NAME, PDF_MIME_TYPE, TEST_FILE_CONTENT.getBytes());

        int initialSize = sqlRepo.getTexts().size();

        sqlRepo.saveFile(file, TEST_FILE_TITLE);

        int currentSize = sqlRepo.getTexts().size();

        assertTrue(currentSize > initialSize, "File should be saved, increasing the text size");
        logger.info("testSaveFile_withValidPDF passed");
    }

    @Test
    public void testSaveFile_withNullFile() {
        MultipartFile file = null;

        assertThrows(IllegalArgumentException.class, () -> {
            sqlRepo.saveFile(file, TEST_FILE_TITLE);
        }, "Saving a null file should throw IllegalArgumentException");

        logger.info("testSaveFile_withNullFile passed");
    }

    @Test
    public void testSaveFile_withNonPDF() {
        MultipartFile file = createMockFile("file", TEST_TXT_NAME, TEXT_MIME_TYPE, TEST_FILE_CONTENT.getBytes());

        assertThrows(IllegalArgumentException.class, () -> {
            sqlRepo.saveFile(file, TEST_FILE_TITLE);
        }, "Saving a non-PDF file should throw IllegalArgumentException");

        logger.info("testSaveFile_withNonPDF passed");
    }

    @Test
    public void testSaveFile_withIOException() throws Exception {
        MultipartFile file = createMockFileThatThrowsIOException();

        assertThrows(FileDataReadingException.class, () -> {
            sqlRepo.saveFile(file, TEST_FILE_TITLE);
        }, "IO exception during file saving should throw FileDataReadingException");

        logger.info("testSaveFile_withIOException passed");
    }

    @Test
    public void testSaveChart() {
        saveChartData(TEST_CHART_LABEL);

        boolean chartExists = isChartExists(TEST_CHART_LABEL);

        assertTrue(chartExists, "Chart should be saved and found in the repository");

        logger.info("testSaveChart passed");
    }

    @Test
    public void testRemoveChart_withExistingLabel() throws Exception {
        saveChartData(TEST_CHART_LABEL);

        int initialSize = chartRepo.findAll().size();

        sqlRepo.removeChart(TEST_CHART_LABEL);

        int currentSize = chartRepo.findAll().size();

        assertTrue(currentSize < initialSize, "Chart should be removed, decreasing the chart size");
        logger.info("testRemoveChart_withExistingLabel passed");
    }

    @Test
    public void testRemoveChart_withNonExistingLabel() {
        assertThrows(ChartNotFoundException.class, () -> {
            sqlRepo.removeChart(NON_EXISTING_CHART_LABEL);
        }, "Removing a non-existing chart should throw ChartNotFoundException");

        logger.info("testRemoveChart_withNonExistingLabel passed");
    }
}
