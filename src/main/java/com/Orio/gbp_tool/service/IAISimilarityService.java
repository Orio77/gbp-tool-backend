package com.Orio.gbp_tool.service;

import java.util.List;

import com.Orio.gbp_tool.model.PDFText;
import com.Orio.gbp_tool.model.SimilarityScore;

public interface IAISimilarityService {

    List<SimilarityScore> calculateScores(List<PDFText> texts, String concept);
}
