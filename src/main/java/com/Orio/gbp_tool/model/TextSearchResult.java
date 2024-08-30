package com.Orio.gbp_tool.model;

import java.util.List;

import lombok.Data;

@Data
public class TextSearchResult {
    private List<PDFText> found;
    private List<String> notFound;
}
