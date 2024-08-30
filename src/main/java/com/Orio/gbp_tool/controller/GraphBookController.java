package com.Orio.gbp_tool.controller;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.Orio.gbp_tool.exception.ChartNotFoundException;
import com.Orio.gbp_tool.exception.ConceptNotRemovedException;
import com.Orio.gbp_tool.exception.FileAlreadyInTheDatabaseException;
import com.Orio.gbp_tool.exception.FileDataReadingException;
import com.Orio.gbp_tool.exception.TextAlreadyInTheDatabaseException;
import com.Orio.gbp_tool.model.ChartData;
import com.Orio.gbp_tool.model.ChartDataResult;
import com.Orio.gbp_tool.model.FileEntity;
import com.Orio.gbp_tool.model.PDFText;
import com.Orio.gbp_tool.model.SimilarityScore;
import com.Orio.gbp_tool.model.TextSearchResult;
import com.Orio.gbp_tool.model.dto.ChartRequest;
import com.Orio.gbp_tool.model.dto.TextRequest;
import com.Orio.gbp_tool.repository.IGraphDatabaseRepo;
import com.Orio.gbp_tool.repository.ISQLRepo;
import com.Orio.gbp_tool.repository.impl.graph.Neo4jRepo;
import com.Orio.gbp_tool.service.IAISimilarityService;
import com.Orio.gbp_tool.service.IChartService;
import com.Orio.gbp_tool.service.ITextProcessorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class GraphBookController {

    private final IGraphDatabaseRepo graphRepo;
    private final ISQLRepo sqlRepo;
    private final ITextProcessorService textProcessorService;
    private final IAISimilarityService aiSimilarityService;
    private final IChartService chartService;
    private static final Logger logger = LoggerFactory.getLogger(Neo4jRepo.class);

    @PostMapping("/add/text")
    public ResponseEntity<String> addText(@RequestBody TextRequest req) {
        FileEntity pdf = null;
        String title = req.getTitle();
        try {
            FileEntity text = sqlRepo.getText(title);
            pdf = text;
        } catch (FileNotFoundException e) {
            logger.error("File not found. Title: {}. Error: {}", title, e);
            return new ResponseEntity<>("File not found", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Unexpected error occurred: {}", e);
            return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<PDFText> text = textProcessorService.createText(pdf);
        try {
            graphRepo.save(text, title);
        } catch (TextAlreadyInTheDatabaseException e) {
            logger.error("Text already in the database. Title: {}. Error: {}", title, e);
            return new ResponseEntity<>("Text already in the database", HttpStatus.FOUND);
        }
        return new ResponseEntity<>(title + " added successfully", HttpStatus.OK);
    }

    @PutMapping("/delete/text")
    public ResponseEntity<String> deleteText(@RequestParam String pdfLabel) {
        logger.info("Request received to delete text with label: {}", pdfLabel);
        try {
            sqlRepo.removeFile(pdfLabel);
            logger.info("Successfully deleted text with label: {}", pdfLabel);
            return new ResponseEntity<>("File deleted successfully", HttpStatus.OK);
        } catch (FileNotFoundException e) {
            logger.error("File not found with label: {}. Exception: {}", pdfLabel, e);
            return new ResponseEntity<>("File not found", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("An error occurred while deleting text with label: {}. Exception: {}", pdfLabel, e);
            return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/add/file")
    public ResponseEntity<String> addFile(@RequestParam("file") MultipartFile file,
            @RequestParam("title") String title) {
        try {
            sqlRepo.saveFile(file, title);
            return new ResponseEntity<>("File added successfully", HttpStatus.CREATED);
        } catch (FileDataReadingException e) {
            logger.error("Exception occurred while reading data from file: {}", e);
            return new ResponseEntity<>("Failed to read file data", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FileAlreadyInTheDatabaseException e) {
            return new ResponseEntity<>("File already in the database", HttpStatus.FOUND);
        }
    }

    @GetMapping("/get/concept/all")
    public ResponseEntity<List<String>> getAllConcepts() {
        List<String> concepts;
        try {
            concepts = graphRepo.getConcepts();
            return ResponseEntity.ok(concepts);
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/text/all")
    public ResponseEntity<List<String>> getTexts() {
        List<FileEntity> texts = new ArrayList<>();
        try {
            texts = sqlRepo.getTexts();
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<String> textNames = texts.stream().map(file -> file.getTitle()).toList();
        return ResponseEntity.ok(textNames);
    }

    @PostMapping("/add/concept")
    public ResponseEntity<String> addConcept(@RequestParam String concept, @RequestParam List<String> textNames) {
        logger.info("Received request to add concept: {} with text names: {}", concept, textNames);

        TextSearchResult result = sqlRepo.getTexts(textNames);

        List<PDFText> nodes = result.getFound();
        List<String> notFound = result.getNotFound();

        logger.info("Text search completed. Found texts: {}, Not found texts: {}", nodes.size(), notFound);

        List<SimilarityScore> scores = aiSimilarityService.calculateScores(nodes, concept);

        graphRepo.addConcept(scores, concept);

        if (nodes.isEmpty()) {
            logger.warn("No texts were found out of the provided: {}", textNames);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No texts were found.");
        } else if (!notFound.isEmpty()) {
            logger.warn("Some texts were not found: {}", notFound);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .body("Concept added successfully. However, the following texts were not found: "
                            + String.join(", ", notFound));
        } else {
            logger.info("Concept added successfully. All texts were found.");
            return ResponseEntity.ok("Concept added successfully. All texts were found.");
        }
    }

    @PutMapping("/delete/concept")
    public ResponseEntity<String> deleteConcept(@RequestParam String name) {
        try {
            boolean isRemoved = graphRepo.removeConcept(name);
            if (isRemoved) {
                return new ResponseEntity<>("Concept removed successfully", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Concept not removed", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (ConceptNotRemovedException e) {
            logger.error("Error removing concept: {}", e.getMessage());
            return new ResponseEntity<>("Failed to remove concept", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/add/chart")
    public ResponseEntity<String> addChart(@RequestBody ChartRequest chartRequest) {
        logger.info("Received request to add chart: {}", chartRequest);

        List<String> concepts = chartRequest.getConcepts();
        String label = chartRequest.getLabel();
        List<String> pdfs = chartRequest.getPdfs();

        if (concepts.isEmpty()) {
            logger.warn("Concepts list is empty.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Concepts list cannot be empty.");
        }

        if (pdfs.isEmpty()) {
            logger.warn("PDFs list is empty.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("PDFs list cannot be empty.");
        }

        try {
            ChartDataResult result = chartService.createChart(concepts, pdfs, label);

            ChartData chartData = result.getChartData();
            List<String> pdfsNotFound = result.getPdfsNotFound();

            sqlRepo.saveChart(chartData);

            if (!pdfsNotFound.isEmpty()) {
                logger.warn("Some PDFs were not found: {}", pdfsNotFound);
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .body("Chart saved successfully. However, the following PDFs were not found: "
                                + String.join(", ", pdfsNotFound));
            } else {
                logger.info("Chart saved successfully. All PDFs were found.");
                return ResponseEntity.ok("Chart saved successfully. All PDFs were found.");
            }
        } catch (Exception e) {
            logger.error("Error creating chart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create chart.");
        }
    }

    @PutMapping("/delete/chart")
    public ResponseEntity<Boolean> deleteChart(@RequestParam String name) {
        try {
            sqlRepo.removeChart(name);
            return ResponseEntity.ok(true);
        } catch (ChartNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }
    }

    @GetMapping("/get/chart")
    public ResponseEntity<ChartData> getChart(@RequestParam String name) {
        try {
            ChartData chartData = sqlRepo.getChart(name);
            return ResponseEntity.ok(chartData);
        } catch (ChartNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}
