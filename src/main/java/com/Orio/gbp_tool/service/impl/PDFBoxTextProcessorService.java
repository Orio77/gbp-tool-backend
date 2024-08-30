package com.Orio.gbp_tool.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.Orio.gbp_tool.model.FileEntity;
import com.Orio.gbp_tool.model.PDFText;
import com.Orio.gbp_tool.service.ITextProcessorService;

@Service
public class PDFBoxTextProcessorService implements ITextProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(PDFBoxTextProcessorService.class);

    @Override
    public List<PDFText> createText(FileEntity file) {
        Assert.notNull(file, "Provided file cannot be null");
        List<PDFText> pdfTexts = new ArrayList<>();
        String title = file.getTitle();
        logger.debug("Starting text extraction from PDF file: {}", title);
        try (PDDocument document = Loader.loadPDF(file.getData())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            int numberOfPages = document.getNumberOfPages();
            logger.debug("Number of pages in the document: {}", numberOfPages);

            for (int page = 1; page <= numberOfPages; page++) {
                pdfStripper.setStartPage(page);
                pdfStripper.setEndPage(page);
                String pageText = pdfStripper.getText(document);
                if (pageText.length() < 200) {
                    logger.debug("Omitted text from page {}: text length is less than 200 characters", page);
                    continue;
                }
                pdfTexts.add(new PDFText(pageText, title, title + page));
                logger.debug("Extracted text from page {}: {}", page, pageText);
            }
        } catch (IOException e) {
            logger.error("Error occurred while processing the PDF file: {}", title, e);
            throw new IllegalArgumentException("Provided text is invalid. Text title: " + title);
        }
        logger.debug("Completed text extraction from PDF file: {}", title);
        return pdfTexts;
    }
}