package com.Orio.gbp_tool.config.converter;

import com.Orio.gbp_tool.model.SimilarityScore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Converter
@RequiredArgsConstructor
public class MapConverter implements AttributeConverter<Map<String, List<SimilarityScore>>, String> {

    private static final Logger logger = LoggerFactory.getLogger(MapConverter.class);
    private final ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(Map<String, List<SimilarityScore>> attribute) {
        try {
            String json = objectMapper.writeValueAsString(attribute);
            logger.debug("Converted map to JSON: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            logger.error("Error converting map to JSON, {}", e);
            throw new IllegalArgumentException("Error converting map to JSON", e);
        }
    }

    @Override
    public Map<String, List<SimilarityScore>> convertToEntityAttribute(String dbData) {
        try {
            Map<String, List<SimilarityScore>> map = objectMapper.readValue(dbData,
                    new TypeReference<Map<String, List<SimilarityScore>>>() {
                    });
            logger.debug("Converted JSON to map: {}", map);
            return map;
        } catch (IOException e) {
            logger.error("Error converting JSON to map, {}", e);
            throw new IllegalArgumentException("Error converting JSON to map", e);
        }
    }
}