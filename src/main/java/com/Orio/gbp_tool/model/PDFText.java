package com.Orio.gbp_tool.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Data;

@Data
public class PDFText implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String text;
    private final String source;
    private final String label;

    @JsonCreator
    public PDFText(@JsonProperty("text") String text, @JsonProperty("source") String source,
            @JsonProperty("label") String label) {
        this.text = text;
        this.source = source;
        this.label = label;
    }
}
