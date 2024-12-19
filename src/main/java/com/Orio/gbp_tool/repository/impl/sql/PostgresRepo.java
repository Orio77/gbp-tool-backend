package com.Orio.gbp_tool.repository.impl.sql;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import com.Orio.gbp_tool.exception.ChartNotFoundException;
import com.Orio.gbp_tool.exception.FileAlreadyInTheDatabaseException;
import com.Orio.gbp_tool.exception.FileDataReadingException;
import com.Orio.gbp_tool.model.ChartData;
import com.Orio.gbp_tool.model.FileEntity;
import com.Orio.gbp_tool.model.PDFText;
import com.Orio.gbp_tool.model.TextSearchResult;
import com.Orio.gbp_tool.repository.ISQLRepo;
import com.Orio.gbp_tool.service.ITextProcessorService;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PostgresRepo implements ISQLRepo {

    private static final Logger logger = LoggerFactory.getLogger(PostgresRepo.class);

    // Constants
    private static final String ERROR_NULL_TITLE = "Provided title cannot be null";
    private static final String ERROR_EMPTY_TITLE = "Provided title cannot be empty";
    private static final String ERROR_NULL_FILE = "Provided file was null";
    private static final String ERROR_NOT_PDF = "File is not a PDF";
    private static final String ERROR_FILE_EXISTS = "File with name: \"%s\" and provided title \"%s\" already exists in the database";
    private static final String ERROR_FILE_NOT_FOUND = "File not found with the following title: %s";
    private static final String ERROR_CHART_NOT_FOUND = "No chart found with the following label \"%s\". Found charts: %s";
    private static final String PDF_EXTENSION = ".pdf";

    private final FileRepo fileRepo;
    @SuppressWarnings("unused")
    private final SimilarityScoreRepo similarityScoreRepo;
    private final ITextProcessorService textProcessorService;
    private final ChartRepo chartRepo;

    @Override
    public void saveFile(MultipartFile file, String title)
            throws IllegalArgumentException, FileDataReadingException, FileAlreadyInTheDatabaseException {
        logger.info("Entering saveFile method with title: \"{}\"", title);
        Assert.notNull(file, ERROR_NULL_FILE);
        validateTitle(title);

        if (!isPDF(file)) {
            logger.warn("File is not a PDF: {}", file.getOriginalFilename());
            throw new IllegalArgumentException(ERROR_NOT_PDF);
        }

        if (this.fileAlreadyExists(file, title)) {
            throw new FileAlreadyInTheDatabaseException(
                    String.format(ERROR_FILE_EXISTS, file.getOriginalFilename(), title));
        }

        FileEntity fileEntity = new FileEntity();
        fileEntity.setTitle(title);
        logger.debug("Title set");
        try {
            logger.debug("Parsing content from the file");
            byte[] fileContent = file.getBytes();
            logger.debug("File Content: {}",
                    Arrays.copyOfRange(fileContent, 0, (5 > fileContent.length ? fileContent.length : 5)));
            fileEntity.setData(fileContent);
            logger.debug("File data set successfully for title: \"{}\"", title);

            fileRepo.save(fileEntity);
            logger.info("File saved successfully with title: \"{}\"", title);
        } catch (IOException e) {
            logger.error("Error reading file data for title: \"{}\", error: {}", title, e);
            throw new FileDataReadingException(e.getMessage(), e.getCause());
        }
        logger.info("Exiting saveFile method with title: \"{}\"", title);
    }

    private boolean fileAlreadyExists(MultipartFile file, String title) throws FileDataReadingException {
        List<FileEntity> allFiles = fileRepo.findAll();

        try {
            byte[] newFileContent = file.getBytes();
            int length = newFileContent.length;
            byte[] newFileFirst100 = Arrays.copyOfRange(newFileContent, 0, Math.min(100, length));
            byte[] newFileLast100 = Arrays.copyOfRange(newFileContent, Math.max(0, length - 100), length);

            return allFiles.stream()
                    .filter(existingFile -> existingFile.getTitle().equals(title))
                    .anyMatch(existingFile -> {
                        byte[] existingFileContent = existingFile.getData();
                        int existingLength = existingFileContent.length;
                        byte[] existingFileFirst100 = Arrays.copyOfRange(existingFileContent, 0,
                                Math.min(100, existingLength));
                        byte[] existingFileLast100 = Arrays.copyOfRange(existingFileContent,
                                Math.max(0, existingLength - 100), existingLength);

                        return Arrays.equals(newFileFirst100, existingFileFirst100)
                                && Arrays.equals(newFileLast100, existingFileLast100);
                    });
        } catch (IOException e) {
            logger.error("Error reading file data for title: \"{}\", error: {}", title, e);
            throw new FileDataReadingException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void removeFile(String title) throws FileNotFoundException {
        logger.info("Attempting to remove file with title: {}", title);

        try {
            FileEntity file = getFile(title);
            fileRepo.deleteById(file.getId());
            logger.info("Successfully removed file with title: {}", title);
        } catch (FileNotFoundException e) {
            logger.error("File with title: {} not found", title, e);
            throw e;
        }
    }

    @Override
    public FileEntity getFile(String title) throws FileNotFoundException {
        validateTitle(title);
        logger.info("Attempting to retrieve file with title: {}", title);

        List<FileEntity> allFiles = fileRepo.findAll();
        logEntityCount(allFiles, "files");

        return findByTitle(allFiles, title, FileEntity::getTitle)
                .orElseThrow(() -> {
                    logger.error(ERROR_FILE_NOT_FOUND, title);
                    return new FileNotFoundException(String.format(ERROR_FILE_NOT_FOUND, title));
                });
    }

    private boolean isPDF(MultipartFile file) {
        return file.getOriginalFilename().toLowerCase().endsWith(PDF_EXTENSION);
    }

    @Override
    public FileEntity getText(String title) throws FileNotFoundException {
        validateTitle(title);
        logger.info("Attempting to retrieve file with title: {}", title);

        List<FileEntity> allFiles = fileRepo.findAll();

        logger.debug("All files: {}", allFiles.stream().map(FileEntity::getTitle).toArray());
        logger.info("Found files count: {}", allFiles.size());

        Optional<FileEntity> file = allFiles.stream()
                .filter(fileEntity -> fileEntity.getTitle().equals(title)).findFirst();

        if (file.isPresent()) {
            logger.info("File found with title: {}", title);
            return file.get();
        } else {
            logger.error("File not found with the following title: {}", title);
            throw new FileNotFoundException("File not found with the following title: " + title);
        }
    }

    public TextSearchResult getTexts(List<String> names) {
        List<FileEntity> texts = new ArrayList<>();
        List<String> notFound = new ArrayList<>();
        TextSearchResult res = new TextSearchResult();

        names.stream().forEach(name -> {
            try {
                texts.add(this.getText(name));
            } catch (FileNotFoundException e) {
                notFound.add(name);
            }
        });

        List<PDFText> nodes = texts.stream().map(text -> textProcessorService.createText(text)).flatMap(List::stream)
                .toList();

        res.setFound(nodes);
        res.setNotFound(notFound);

        return res;
    }

    @Override
    public List<FileEntity> getTexts() throws Exception {
        return fileRepo.findAll();
    }

    @Override
    public void saveChart(ChartData data) {
        chartRepo.save(data);
    }

    @Override
    public void removeChart(String label) throws ChartNotFoundException {
        ChartData chart = getChart(label);
        chartRepo.deleteById(chart.getId());
    }

    @Override
    public ChartData getChart(String label) throws ChartNotFoundException {
        validateTitle(label);
        logger.info("Attempting to retrieve chart with label: {}", label);

        List<ChartData> charts = chartRepo.findAll();
        logEntityCount(charts, "charts");

        return findByTitle(charts, label, ChartData::getLabel)
                .orElseThrow(() -> {
                    List<String> labels = charts.stream().map(ChartData::getLabel).toList();
                    logger.error(ERROR_CHART_NOT_FOUND, label, labels);
                    return new ChartNotFoundException(String.format(ERROR_CHART_NOT_FOUND, label, labels));
                });
    }

    /**
     * Common validation logic for title parameter
     */
    private void validateTitle(String title) {
        Assert.notNull(title, ERROR_NULL_TITLE);
        Assert.hasText(title, ERROR_EMPTY_TITLE);
    }

    /**
     * Common logic for finding an entity by title
     */
    private <T> Optional<T> findByTitle(List<T> entities, String title, Function<T, String> titleExtractor) {
        return entities.stream()
                .filter(entity -> titleExtractor.apply(entity).equals(title))
                .findFirst();
    }

    /**
     * Helper method to log entity counts
     */
    private <T> void logEntityCount(List<T> entities, String entityType) {
        logger.info("Found {} count: {}", entityType, entities.size());
    }
}
