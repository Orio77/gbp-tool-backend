package com.Orio.gbp_tool.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class OllamaResponse {
    private final String analysis;
    private final double score;

    @JsonCreator
    public OllamaResponse(@JsonProperty("analysis") String analysis, @JsonProperty("score") double score) {
        this.analysis = analysis;
        this.score = score;
    }
}
