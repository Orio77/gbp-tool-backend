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

    private final OllamaApi ollama;
    private final OllamaConfig config;
    private final ObjectMapper objMapper;
    private static final Logger logger = LoggerFactory.getLogger(OllamaSimilarityService.class);

    @Override
    public List<SimilarityScore> calculateScores(List<PDFText> texts, String concept) { // TODO add periodic saves
        logger.info("Starting calculateScores method.");
        Assert.notNull(texts, "The 'texts' list must not be null.");
        Assert.notEmpty(texts, "The 'texts' list must not be empty.");
        Assert.notNull(concept, "The 'concept' string must not be null.");

        logger.debug("Input texts: {}, concept: {}", texts, concept);

        List<SimilarityScore> scores = new ArrayList<>();

        for (PDFText text : texts) {
            logger.debug("Processing text: {}", text.getText());
            ChatRequest request = ChatRequest.builder(config.getModel()).withFormat("json")
                    .withMessages(this.getMessages(text.getText(), concept)).build();
            logger.debug("Sending chat request: {}", request);
            ChatResponse response = ollama.chat(request);

            String content = response.message().content();
            logger.debug("Received response content: {}", content);

            OllamaResponse json = parseJson(content);

            if (json != null) {
                scores.add(new SimilarityScore(text, concept, json.getScore()));
            } else {
                logger.warn("Parsed JSON is null for content: {}", content);
            }
        }

        logger.info("Finished calculateScores method. Scores: {}", scores);
        return scores;
    }

    private List<Message> getMessages(String text, String concept) {
        Message systemActAs = Message.builder(Role.SYSTEM).withContent(
                "Act as a similarity score calculator based on meaning. Return the score as double in range 0-100. The score an indicator of how much the meaning of text matches the concept provided")
                .build();
        Message systemResponseFormat = Message.builder(Role.SYSTEM).withContent(
                "Respond in the following json format: {\"analysis\": \"Brief 3 sentence analysis of the meaning of the text\", \"score\": \"Similarity Score in range (0.0-100.0)\"}")
                .build();
        Message userTextConcept = Message.builder(Role.USER)
                .withContent(String.format("TEXT:\"\"\"%s\"\"\"\n\nConcept: \"%s\"", text, concept)).build();

        return Arrays.asList(systemActAs, systemResponseFormat, userTextConcept);
    }

    private OllamaResponse parseJson(String content) {
        try {
            OllamaResponse responseModel = objMapper.readValue(content,
                    OllamaResponse.class);
            return responseModel;
        } catch (JsonProcessingException e) {
            logger.error("Error occurred while parsing json: {}, Response: {}", e.getMessage(),
                    content);
            return null;
        }
    }

}