package com.Orio.gbp_tool.repository;

import com.Orio.gbp_tool.exception.ConceptNotRemovedException;
import com.Orio.gbp_tool.exception.TextAlreadyInTheDatabaseException;
import com.Orio.gbp_tool.model.PDFText;
import com.Orio.gbp_tool.model.SimilarityScore;

import java.util.List;

public interface IGraphDatabaseRepo {

    void save(List<PDFText> texts, String label) throws TextAlreadyInTheDatabaseException;

    void removeText(String label);

    List<String> getConcepts() throws Exception;

    void addConcept(List<SimilarityScore> scores, String concept);

    boolean removeConcept(String concept) throws ConceptNotRemovedException;
}
