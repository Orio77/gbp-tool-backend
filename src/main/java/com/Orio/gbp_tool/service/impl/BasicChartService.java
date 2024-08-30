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

    @Override
    public ChartDataResult createChart(List<String> concepts, List<String> pdfNames, String label)
            throws NoPdfFoundException {
        Assert.notNull(concepts, "Concepts list must not be null");
        Assert.notEmpty(concepts, "Concepts list must not be empty");
        Assert.notNull(pdfNames, "PDF names list must not be null");
        Assert.notEmpty(pdfNames, "PDF names list must not be empty");
        Assert.hasText(label, "Label must not be null or empty");

        logger.info("Creating chart with label: {}", label);
        logger.debug("Fetching texts for PDF names: {}", pdfNames);

        TextSearchResult texts = sqlRepo.getTexts(pdfNames);
        List<PDFText> found = texts.getFound();
        List<String> notFound = texts.getNotFound();
        ChartDataResult res = new ChartDataResult();

        if (found.isEmpty()) {
            logger.warn("No PDFs found for the provided names: {}", pdfNames);
            throw new NoPdfFoundException(String.format("No pdfs were found out of the provided %s", pdfNames));
        }
        logger.debug("Found PDFs: {}", found);
        logger.debug("Not found PDFs: {}", notFound);

        ChartData chartData = new ChartData();
        chartData.setData(new HashMap<>());

        for (String concept : concepts) {
            logger.debug("Calculating scores for concept: {}", concept);
            List<SimilarityScore> calculatedScores = aISimilarityService.calculateScores(found, concept);
            logger.debug("Scores for concept {}: {}", concept, calculatedScores);
            chartData.getData().put(concept, calculatedScores);
        }

        chartData.setLabel(label);

        res.setChartData(chartData);
        res.setPdfsNotFound(notFound);

        logger.info("Chart creation complete for label: {}", label);
        logger.debug("ChartDataResult: {}", res);
        return res;
    }

}