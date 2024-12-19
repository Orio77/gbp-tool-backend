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

    // Constants
    private static final int MIN_TEXT_LENGTH = 200;
    private static final String VALIDATION_FILE_NOT_NULL_MSG = "Provided file cannot be null";
    private static final String EXCEPTION_INVALID_TEXT_TEMPLATE = "Provided text is invalid. Text title: %s";

    @Override
    public List<PDFText> createText(FileEntity file) {
        validateFile(file);

        List<PDFText> pdfTexts = new ArrayList<>();
        String title = file.getTitle();
        logger.debug("Starting text extraction from PDF file: {}", title);

        try (PDDocument document = loadPDDocument(file.getData(), title)) {
            PDFTextStripper pdfStripper = initializePDFTextStripper();
            int numberOfPages = getNumberOfPages(document);
            logger.debug("Number of pages in the document: {}", numberOfPages);

            for (int page = 1; page <= numberOfPages; page++) {
                String pageText = extractTextFromPage(document, page, pdfStripper);
                if (isTextTooShort(pageText)) {
                    logger.debug("Omitted text from page {}: text length is less than {} characters", page,
                            MIN_TEXT_LENGTH);
                    continue;
                }
                PDFText pdfText = createPDFText(pageText, title, page);
                pdfTexts.add(pdfText);
                logger.debug("Extracted text from page {}: {}", page, truncateText(pageText));
            }
        } catch (IOException e) {
            logger.error("Error occurred while processing the PDF file: {}", title, e);
            throw new IllegalArgumentException(String.format(EXCEPTION_INVALID_TEXT_TEMPLATE, title), e);
        }

        logger.debug("Completed text extraction from PDF file: {}", title);
        return pdfTexts;
    }

    /**
     * Validates the provided FileEntity.
     *
     * @param file The FileEntity to validate.
     */
    private void validateFile(FileEntity file) {
        Assert.notNull(file, VALIDATION_FILE_NOT_NULL_MSG);
    }

    /**
     * Loads a PDDocument from the provided byte array.
     *
     * @param data  The byte array of the PDF.
     * @param title The title of the PDF file.
     * @return A loaded PDDocument.
     * @throws IOException If an error occurs while loading the PDF.
     */
    private PDDocument loadPDDocument(byte[] data, String title) throws IOException {
        try {
            return Loader.loadPDF(data);
        } catch (IOException e) {
            logger.error("Failed to load PDF document: {}", title, e);
            throw e;
        }
    }

    /**
     * Initializes and returns a PDFTextStripper instance.
     *
     * @return An initialized PDFTextStripper.
     * @throws IOException If an error occurs during initialization.
     */
    private PDFTextStripper initializePDFTextStripper() throws IOException {
        PDFTextStripper pdfStripper = new PDFTextStripper();
        // Additional configurations for pdfStripper can be added here if needed
        return pdfStripper;
    }

    /**
     * Retrieves the number of pages in the PDDocument.
     *
     * @param document The PDDocument.
     * @return The number of pages.
     */
    private int getNumberOfPages(PDDocument document) {
        return document.getNumberOfPages();
    }

    /**
     * Extracts text from a specific page in the PDDocument.
     *
     * @param document    The PDDocument.
     * @param page        The page number to extract text from.
     * @param pdfStripper The PDFTextStripper instance.
     * @return The extracted text.
     * @throws IOException If an error occurs during text extraction.
     */
    private String extractTextFromPage(PDDocument document, int page, PDFTextStripper pdfStripper) throws IOException {
        pdfStripper.setStartPage(page);
        pdfStripper.setEndPage(page);
        return pdfStripper.getText(document).trim();
    }

    /**
     * Checks if the extracted text is shorter than the minimum required length.
     *
     * @param text The extracted text.
     * @return True if text length is less than MIN_TEXT_LENGTH, else false.
     */
    private boolean isTextTooShort(String text) {
        return text.length() < MIN_TEXT_LENGTH;
    }

    /**
     * Creates a PDFText instance from the extracted text.
     *
     * @param text  The extracted text.
     * @param title The title of the PDF file.
     * @param page  The page number.
     * @return A new PDFText instance.
     */
    private PDFText createPDFText(String text, String title, int page) {
        String pageLabel = title + page;
        return new PDFText(text, title, pageLabel);
    }

    /**
     * Truncates the text for logging purposes to avoid excessively long log
     * messages.
     *
     * @param text The text to truncate.
     * @return A truncated version of the text.
     */
    private String truncateText(String text) {
        int maxLength = 100;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
