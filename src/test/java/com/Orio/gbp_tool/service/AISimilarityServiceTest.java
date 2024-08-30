package com.Orio.gbp_tool.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.Orio.gbp_tool.model.PDFText;
import com.Orio.gbp_tool.model.SimilarityScore;

@SpringBootTest
public class AISimilarityServiceTest {

    @Autowired
    private IAISimilarityService aISimilarityService;

    @Test
    public void testCalculateScores_withValidInputs() {
        PDFText text1 = new PDFText("Sample text 1", "Some source", "label1");
        PDFText text2 = new PDFText("Sample text 2", "Some source", "label2");

        List<PDFText> texts = Arrays.asList(text1, text2);
        String concept = "Sample concept";

        List<SimilarityScore> scores = aISimilarityService.calculateScores(texts, concept);

        assertEquals(2, scores.size());
    }

    @Test
    public void testCalculateScores_withNullTexts() {
        String concept = "Sample concept";
        assertThrows(IllegalArgumentException.class, () -> aISimilarityService.calculateScores(null, concept));
    }

    @Test
    public void testCalculateScores_withEmptyTexts() {
        List<PDFText> texts = Arrays.asList();
        String concept = "Sample concept";
        assertThrows(IllegalArgumentException.class, () -> aISimilarityService.calculateScores(texts, concept));
    }

    @Test
    public void testCalculateScores_withNullConcept() {
        PDFText text1 = new PDFText("Sample text 1", "Some source", "label1");

        List<PDFText> texts = Arrays.asList(text1);
        assertThrows(IllegalArgumentException.class, () -> aISimilarityService.calculateScores(texts, null));
    }
}