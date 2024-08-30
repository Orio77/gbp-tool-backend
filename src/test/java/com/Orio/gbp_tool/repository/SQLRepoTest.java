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

    private final String fileTitle = "Test Title";
    private final String chartLabel = "Test Chart";

    @AfterEach
    public void tearDown() {
        // Clean up any data if necessary
        logger.info("Cleaning up after test");
        try {
            sqlRepo.removeFile(fileTitle);
        } catch (FileNotFoundException e) {
            logger.info(e.getMessage());
        }

        try {
            sqlRepo.removeChart(chartLabel);
        } catch (ChartNotFoundException e) {
            logger.info(e.getMessage());
        }
    }

    @Test
    public void testSaveFile_withValidPDF()
            throws IOException, FileDataReadingException, FileAlreadyInTheDatabaseException, Exception {
        MultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "test content".getBytes());

        int initialSize = sqlRepo.getTexts().size();

        sqlRepo.saveFile(file, fileTitle);

        int curSize = sqlRepo.getTexts().size();

        assertTrue(curSize > initialSize);
        logger.info("testSaveFile_withValidPDF passed");
    }

    @Test
    public void testSaveFile_withNullFile() {
        MultipartFile file = null;

        assertThrows(IllegalArgumentException.class, () -> {
            sqlRepo.saveFile(file, fileTitle);
        });

        logger.info("testSaveFile_withNullFile passed");
    }

    @Test
    public void testSaveFile_withNonPDF() {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());

        assertThrows(IllegalArgumentException.class, () -> {
            sqlRepo.saveFile(file, fileTitle);
        });

        logger.info("testSaveFile_withNonPDF passed");
    }

    @Test
    public void testSaveFile_withIOException() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("IO Exception");
            }
        };

        assertThrows(FileDataReadingException.class, () -> {
            sqlRepo.saveFile(file, fileTitle);
        });

        logger.info("testSaveFile_withIOException passed");
    }

    @Test
    public void testSaveChart() {
        ChartData chartData = new ChartData();
        chartData.setLabel(chartLabel);

        sqlRepo.saveChart(chartData);

        List<ChartData> charts = chartRepo.findAll();
        boolean chartExists = charts.stream().anyMatch(chart -> chart.getLabel().equals(chartLabel));

        assertTrue(chartExists, "Chart should be saved and found in the repository");

        logger.info("testSaveChart passed");
    }

    @Test
    public void testRemoveChart_withExistingLabel() throws ChartNotFoundException, Exception {
        ChartData chartData = new ChartData();
        chartData.setLabel(chartLabel);
        sqlRepo.saveChart(chartData);

        int initialSize = chartRepo.findAll().size();

        sqlRepo.removeChart(chartLabel);

        int curSize = chartRepo.findAll().size();

        assertTrue(curSize < initialSize);
        logger.info("testRemoveChart_withExistingLabel passed");
    }

    @Test
    public void testRemoveChart_withNonExistingLabel() {
        assertThrows(ChartNotFoundException.class, () -> {
            sqlRepo.removeChart("Non Existing Chart");
        });

        logger.info("testRemoveChart_withNonExistingLabel passed");
    }
}