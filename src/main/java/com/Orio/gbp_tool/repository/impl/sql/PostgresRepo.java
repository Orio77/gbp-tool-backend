package com.Orio.gbp_tool.repository.impl.sql;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

    private final FileRepo fileRepo;
    @SuppressWarnings("unused")
    private final SimilarityScoreRepo similarityScoreRepo;
    private final ITextProcessorService textProcessorService;
    private final ChartRepo chartRepo;

    @Override
    public void saveFile(MultipartFile file, String title)
            throws IllegalArgumentException, FileDataReadingException, FileAlreadyInTheDatabaseException {
        logger.info("Entering saveFile method with title: \"{}\"", title);
        Assert.notNull(file, "Provided file was null");
        Assert.notNull(title, "Provided title was empty");
        Assert.hasText(title, "Provided title cannot be empty");

        if (!isPDF(file)) {
            logger.warn("File is not a PDF: {}", file.getOriginalFilename());
            throw new IllegalArgumentException("File is not a PDF");
        }

        if (this.fileAlreadyExists(file, title)) {
            throw new FileAlreadyInTheDatabaseException(
                    String.format("File with name: \"%s\" and provided title \"%s\" already exists in the database",
                            file.getOriginalFilename(), title));
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
        Assert.notNull(title, "Provided title cannot be null");
        Assert.hasText(title, "Provided title cannot be empty");
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

    private boolean isPDF(MultipartFile file) {
        return file.getOriginalFilename().toLowerCase().endsWith(".pdf");
    }

    @Override
    public FileEntity getText(String title) throws FileNotFoundException {
        Assert.notNull(title, "Provided title cannot be null");
        Assert.hasText(title, "Provided title cannot be empty");
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
        Assert.notNull(label, "Provided label cannot be null");
        Assert.hasText(label, "Provided label cannot be empty");
        logger.info("Attempting to retrieve chart with label: {}", label);

        List<ChartData> charts = chartRepo.findAll();
        logger.debug("All charts: {}", charts.stream().map(ChartData::getLabel).toArray());
        logger.info("Found charts count: {}", charts.size());

        Optional<ChartData> foundChart = charts.stream().filter(chart -> chart.getLabel().equals(label)).findFirst();

        if (foundChart.isPresent()) {
            logger.info("Chart found with label: {}", label);
            return foundChart.get();
        } else {
            List<String> labels = charts.stream().map(ChartData::getLabel).toList();
            logger.error("No chart found with the following label: {}. Found charts: {}", label, labels);
            throw new ChartNotFoundException(
                    String.format("No chart found with the following label \"%s\". Found charts: %s", label, labels));
        }
    }

}
