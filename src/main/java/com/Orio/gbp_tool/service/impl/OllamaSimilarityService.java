package com.Orio.gbp_tool.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaApi.ChatRequest;
import org.springframework.ai.ollama.api.OllamaApi.ChatResponse;
import org.springframework.ai.ollama.api.OllamaApi.Message;
import org.springframework.ai.ollama.api.OllamaApi.Message.Role;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.Orio.gbp_tool.config.OllamaConfig;
import com.Orio.gbp_tool.model.OllamaResponse;
import com.Orio.gbp_tool.model.PDFText;
import com.Orio.gbp_tool.model.SimilarityScore;
import com.Orio.gbp_tool.service.IAISimilarityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OllamaSimilarityService implements IAISimilarityService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaSimilarityService.class);

    // Constants for message content
    private static final String FORMAT_JSON = "json";
    private static final String SYSTEM_ACT_AS_CONTENT = "Act as a similarity score calculator based on meaning. Return the score as double in range 0-100. The score an indicator of how much the meaning of text matches the concept provided";
    private static final String SYSTEM_RESPONSE_FORMAT_CONTENT = "Respond in the following json format: {\"analysis\": \"Brief 3 sentence analysis of the meaning of the text\", \"score\": \"Similarity Score in range (0.0-100.0)\"}";
    private static final String TEXT_CONCEPT_TEMPLATE = "TEXT:\"\"\"%s\"\"\"\n\nConcept: \"%s\"";

    // Constants for exception messages
    private static final String JSON_PARSING_ERROR_MSG = "Error occurred while parsing json: {}, Response: {}";

    private final OllamaApi ollama;
    private final OllamaConfig config;
    private final ObjectMapper objMapper;

    @Override
    public List<SimilarityScore> calculateScores(List<PDFText> texts, String concept) { // TODO add periodic saves
        logger.info("Starting calculateScores method.");
        validateInputs(texts, concept);

        logger.debug("Input texts: {}, concept: {}", texts, concept);

        List<SimilarityScore> scores = new ArrayList<>();

        for (PDFText text : texts) {
            logger.debug("Processing text: {}", text.getText());
            ChatRequest request = buildChatRequest(text.getText(), concept);
            logger.debug("Sending chat request: {}", request);
            ChatResponse response = ollama.chat(request);

            String content = response.message().content();
            logger.debug("Received response content: {}", content);

            OllamaResponse json = parseJson(content);

            if (json != null) {
                SimilarityScore score = createSimilarityScore(text, concept, json);
                scores.add(score);
            } else {
                logger.warn("Parsed JSON is null for content: {}", content);
            }
        }

        logger.info("Finished calculateScores method. Scores: {}", scores);
        return scores;
    }

    /**
     * Validates the input parameters.
     *
     * @param texts   List of PDFText objects.
     * @param concept The concept string.
     */
    private void validateInputs(List<PDFText> texts, String concept) {
        Assert.notNull(texts, "The 'texts' list must not be null.");
        Assert.notEmpty(texts, "The 'texts' list must not be empty.");
        Assert.notNull(concept, "The 'concept' string must not be null.");
    }

    /**
     * Builds the ChatRequest for a given text and concept.
     *
     * @param text    The text content.
     * @param concept The concept to compare.
     * @return A ChatRequest object.
     */
    private ChatRequest buildChatRequest(String text, String concept) {
        List<Message> messages = createMessages(text, concept);
        return ChatRequest.builder(config.getModel())
                .withFormat(FORMAT_JSON)
                .withMessages(messages)
                .build();
    }

    /**
     * Creates the list of messages for the ChatRequest.
     *
     * @param text    The text content.
     * @param concept The concept to compare.
     * @return A list of Message objects.
     */
    private List<Message> createMessages(String text, String concept) {
        Message systemActAs = Message.builder(Role.SYSTEM)
                .withContent(SYSTEM_ACT_AS_CONTENT)
                .build();

        Message systemResponseFormat = Message.builder(Role.SYSTEM)
                .withContent(SYSTEM_RESPONSE_FORMAT_CONTENT)
                .build();

        Message userTextConcept = Message.builder(Role.USER)
                .withContent(String.format(TEXT_CONCEPT_TEMPLATE, text, concept))
                .build();

        return Arrays.asList(systemActAs, systemResponseFormat, userTextConcept);
    }

    /**
     * Parses the JSON content into an OllamaResponse object.
     *
     * @param content The JSON content string.
     * @return The OllamaResponse object, or null if parsing fails.
     */
    private OllamaResponse parseJson(String content) {
        try {
            return objMapper.readValue(content, OllamaResponse.class);
        } catch (JsonProcessingException e) {
            logger.error(JSON_PARSING_ERROR_MSG, e.getMessage(), content);
            return null;
        }
    }

    /**
     * Creates a SimilarityScore object from the given text, concept, and
     * OllamaResponse.
     *
     * @param text     The PDFText object.
     * @param concept  The concept string.
     * @param response The OllamaResponse object.
     * @return A SimilarityScore object.
     */
    private SimilarityScore createSimilarityScore(PDFText text, String concept, OllamaResponse response) {
        return new SimilarityScore(text, concept, response.getScore());
    }
}
