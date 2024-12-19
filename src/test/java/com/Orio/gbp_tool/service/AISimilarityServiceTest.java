package com.Orio.gbp_tool.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// Consider importing only necessary annotations if possible
import org.springframework.boot.test.context.SpringBootTest;

import com.Orio.gbp_tool.model.PDFText;
import com.Orio.gbp_tool.model.SimilarityScore;

@SpringBootTest
public class AISimilarityServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(AISimilarityServiceTest.class);

    @Autowired
    private IAISimilarityService aISimilarityService;

    // Constants
    private static final String SAMPLE_TEXT_CONTENT_1 = "Sample text 1";
    private static final String SAMPLE_TEXT_CONTENT_2 = "Sample text 2";
    private static final String SAMPLE_SOURCE = "Some source";
    private static final String LABEL_1 = "label1";
    private static final String LABEL_2 = "label2";
    private static final String SAMPLE_CONCEPT = "Sample concept";
    private static final int EXPECTED_SCORE_SIZE_VALID = 2;

    @Test
    public void testCalculateScores_withValidInputs() {
        List<PDFText> texts = createPDFTexts(
                createPDFText(SAMPLE_TEXT_CONTENT_1, SAMPLE_SOURCE, LABEL_1),
                createPDFText(SAMPLE_TEXT_CONTENT_2, SAMPLE_SOURCE, LABEL_2));

        List<SimilarityScore> scores = aISimilarityService.calculateScores(texts, SAMPLE_CONCEPT);

        assertEquals(EXPECTED_SCORE_SIZE_VALID, scores.size(), "Scores size should match the number of input texts");
        logger.info("testCalculateScores_withValidInputs passed");
    }

    @Test
    public void testCalculateScores_withNullTexts() {
        String concept = SAMPLE_CONCEPT;
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            aISimilarityService.calculateScores(null, concept);
        }, "Calculating scores with null texts should throw IllegalArgumentException");

        logger.info("testCalculateScores_withNullTexts passed with exception: {}", exception.getMessage());
    }

    @Test
    public void testCalculateScores_withEmptyTexts() {
        List<PDFText> texts = Collections.emptyList();
        String concept = SAMPLE_CONCEPT;

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            aISimilarityService.calculateScores(texts, concept);
        }, "Calculating scores with empty texts should throw IllegalArgumentException");

        logger.info("testCalculateScores_withEmptyTexts passed with exception: {}", exception.getMessage());
    }

    @Test
    public void testCalculateScores_withNullConcept() {
        PDFText text = createPDFText(SAMPLE_TEXT_CONTENT_1, SAMPLE_SOURCE, LABEL_1);
        List<PDFText> texts = Collections.singletonList(text);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            aISimilarityService.calculateScores(texts, null);
        }, "Calculating scores with null concept should throw IllegalArgumentException");

        logger.info("testCalculateScores_withNullConcept passed with exception: {}", exception.getMessage());
    }

    // Private helper methods

    /**
     * Creates a PDFText instance with the given parameters.
     *
     * @param content The content of the PDF text.
     * @param source  The source of the PDF text.
     * @param label   The label of the PDF text.
     * @return A new PDFText instance.
     */
    private PDFText createPDFText(String content, String source, String label) {
        return new PDFText(content, source, label);

    }

    /**
     * Creates a list of PDFText instances.
     *
     * @param pdfTexts Varargs of PDFText instances.
     * @return A list containing the provided PDFText instances.
     */
    private List<PDFText> createPDFTexts(PDFText... pdfTexts) {
        return Arrays.asList(pdfTexts);
    }
}
