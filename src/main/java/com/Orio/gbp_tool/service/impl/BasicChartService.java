package com.Orio.gbp_tool.service.impl;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.Orio.gbp_tool.exception.NoPdfFoundException;
import com.Orio.gbp_tool.model.ChartData;
import com.Orio.gbp_tool.model.ChartDataResult;
import com.Orio.gbp_tool.model.PDFText;
import com.Orio.gbp_tool.model.SimilarityScore;
import com.Orio.gbp_tool.model.TextSearchResult;
import com.Orio.gbp_tool.repository.ISQLRepo;
import com.Orio.gbp_tool.service.IAISimilarityService;
import com.Orio.gbp_tool.service.IChartService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BasicChartService implements IChartService {

    private static final Logger logger = LoggerFactory.getLogger(BasicChartService.class);

    private final IAISimilarityService aISimilarityService;
    private final ISQLRepo sqlRepo;

    // Constants for validation messages
    private static final String CONCEPTS_NOT_NULL_MSG = "Concepts list must not be null";
    private static final String CONCEPTS_NOT_EMPTY_MSG = "Concepts list must not be empty";
    private static final String PDF_NAMES_NOT_NULL_MSG = "PDF names list must not be null";
    private static final String PDF_NAMES_NOT_EMPTY_MSG = "PDF names list must not be empty";
    private static final String LABEL_NOT_EMPTY_MSG = "Label must not be null or empty";

    // Constants for exception messages
    private static final String NO_PDFS_FOUND_MSG_TEMPLATE = "No pdfs were found out of the provided %s";

    @Override
    public ChartDataResult createChart(List<String> concepts, List<String> pdfNames, String label)
            throws NoPdfFoundException {
        validateInputs(concepts, pdfNames, label);

        logger.info("Creating chart with label: {}", label);
        logger.debug("Fetching texts for PDF names: {}", pdfNames);

        TextSearchResult texts = sqlRepo.getTexts(pdfNames);
        List<PDFText> found = texts.getFound();
        List<String> notFound = texts.getNotFound();

        handleNoPdfsFound(found, pdfNames);

        ChartData chartData = buildChartData(concepts, found);
        chartData.setLabel(label);

        ChartDataResult result = buildChartDataResult(chartData, notFound);

        logger.info("Chart creation complete for label: {}", label);
        logger.debug("ChartDataResult: {}", result);

        return result;
    }

    /**
     * Validates the input parameters for creating a chart.
     *
     * @param concepts List of concepts.
     * @param pdfNames List of PDF names.
     * @param label    Label for the chart.
     */
    private void validateInputs(List<String> concepts, List<String> pdfNames, String label) {
        Assert.notNull(concepts, CONCEPTS_NOT_NULL_MSG);
        Assert.notEmpty(concepts, CONCEPTS_NOT_EMPTY_MSG);
        Assert.notNull(pdfNames, PDF_NAMES_NOT_NULL_MSG);
        Assert.notEmpty(pdfNames, PDF_NAMES_NOT_EMPTY_MSG);
        Assert.hasText(label, LABEL_NOT_EMPTY_MSG);
    }

    /**
     * Handles the scenario where no PDFs are found based on the provided PDF names.
     *
     * @param found    List of found PDF texts.
     * @param pdfNames List of PDF names requested.
     * @throws NoPdfFoundException If no PDFs are found.
     */
    private void handleNoPdfsFound(List<PDFText> found, List<String> pdfNames) throws NoPdfFoundException {
        if (found.isEmpty()) {
            logger.warn("No PDFs found for the provided names: {}", pdfNames);
            throw new NoPdfFoundException(String.format(NO_PDFS_FOUND_MSG_TEMPLATE, pdfNames));
        }
        logger.debug("Found PDFs: {}", found);
        logger.debug("Not found PDFs: {}", sqlRepo.getTexts(pdfNames).getNotFound());
    }

    /**
     * Builds the ChartData object by calculating similarity scores for each
     * concept.
     *
     * @param concepts List of concepts.
     * @param found    List of found PDF texts.
     * @return A populated ChartData object.
     */
    private ChartData buildChartData(List<String> concepts, List<PDFText> found) {
        ChartData chartData = new ChartData();
        chartData.setData(new HashMap<>());

        for (String concept : concepts) {
            logger.debug("Calculating scores for concept: {}", concept);
            List<SimilarityScore> calculatedScores = aISimilarityService.calculateScores(found, concept);
            logger.debug("Scores for concept {}: {}", concept, calculatedScores);
            chartData.getData().put(concept, calculatedScores);
        }

        return chartData;
    }

    /**
     * Constructs the ChartDataResult object.
     *
     * @param chartData ChartData object containing the chart information.
     * @param notFound  List of PDF names that were not found.
     * @return A populated ChartDataResult object.
     */
    private ChartDataResult buildChartDataResult(ChartData chartData, List<String> notFound) {
        ChartDataResult result = new ChartDataResult();
        result.setChartData(chartData);
        result.setPdfsNotFound(notFound);
        return result;
    }
}
