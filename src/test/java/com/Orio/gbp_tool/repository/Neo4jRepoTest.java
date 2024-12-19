package com.Orio.gbp_tool.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import com.Orio.gbp_tool.exception.ConceptNotRemovedException;
import com.Orio.gbp_tool.exception.TextAlreadyInTheDatabaseException;
import com.Orio.gbp_tool.model.Concept;
import com.Orio.gbp_tool.model.PDFText;
import com.Orio.gbp_tool.model.SimilarityScore;
import com.Orio.gbp_tool.repository.impl.graph.Neo4jRepo;

@SpringBootTest
public class Neo4jRepoTest {

    @Autowired
    private Neo4jRepo neo4jRepo;

    @Autowired
    private Driver driver;

    private int nodeCount;

    // Private Helper Methods

    /**
     * Executes a count query without parameters.
     *
     * @param query The Cypher query to execute.
     * @return The count result as an integer.
     */
    private int executeCountQuery(String query) {
        try (Session session = driver.session()) {
            Result result = session.run(query);
            return result.single().get("count").asInt();
        }
    }

    /**
     * Executes a count query with a single parameter.
     *
     * @param query      The Cypher query to execute.
     * @param paramName  The name of the parameter.
     * @param paramValue The value of the parameter.
     * @return The count result as an integer.
     */
    private int executeCountQuery(String query, String paramName, String paramValue) {
        try (Session session = driver.session()) {
            Result result = session.run(query, Values.parameters(paramName, paramValue));
            return result.single().get("count").asInt();
        }
    }

    /**
     * Executes a query without parameters.
     *
     * @param query The Cypher query to execute.
     */
    private void executeQuery(String query) {
        try (Session session = driver.session()) {
            session.run(query);
        }
    }

    /**
     * Executes a query with a single parameter.
     *
     * @param query      The Cypher query to execute.
     * @param paramName  The name of the parameter.
     * @param paramValue The value of the parameter.
     */
    private void executeQuery(String query, String paramName, String paramValue) {
        try (Session session = driver.session()) {
            session.run(query, Values.parameters(paramName, paramValue));
        }
    }

    /**
     * Cleans the database by deleting test nodes and relationships.
     */
    private void cleanDatabase() {
        try (Session session = driver.session()) {
            executeQuery(String.format(VectorRepoConstants.QUERY_DELETE_TEST_LABEL, VectorRepoConstants.TEST_LABEL));
            executeQuery(VectorRepoConstants.QUERY_DELETE_CONCEPT, "concept", VectorRepoConstants.TEST_CONCEPT);
            executeQuery(VectorRepoConstants.QUERY_DELETE_CONTAINING_NAME, "name", "Test");
            executeQuery(VectorRepoConstants.QUERY_DELETE_CONTAINING_NAME, "name", "TestSource");
        }
    }

    @BeforeEach
    public void setUp() {
        tearDown(); // Ensure a clean state before each test
        nodeCount = executeCountQuery(VectorRepoConstants.QUERY_COUNT_ALL);
    }

    @AfterEach
    public void tearDown() {
        cleanDatabase(); // Clean up after each test
    }

    @Test
    public void testSave_withValidData() throws TextAlreadyInTheDatabaseException {
        List<PDFText> texts = Arrays.asList(
                new PDFText(VectorRepoConstants.CONTENT_1, VectorRepoConstants.SOME_SOURCE,
                        VectorRepoConstants.SOURCE_1),
                new PDFText(VectorRepoConstants.CONTENT_2, VectorRepoConstants.SOME_SOURCE,
                        VectorRepoConstants.SOURCE_2));

        neo4jRepo.save(texts, VectorRepoConstants.TEST_LABEL);

        assertEquals(nodeCount + texts.size(), executeCountQuery(VectorRepoConstants.QUERY_COUNT_ALL));
    }

    @Test
    public void testSave_withEmptyList() throws TextAlreadyInTheDatabaseException {
        List<PDFText> texts = Collections.emptyList();

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> neo4jRepo.save(texts, VectorRepoConstants.TEST_LABEL));

        assertEquals(nodeCount, executeCountQuery(VectorRepoConstants.QUERY_COUNT_ALL));
    }

    @Test
    public void testRemoveText_withValidLabel() throws TextAlreadyInTheDatabaseException {
        // Given
        List<PDFText> texts = Arrays.asList(
                new PDFText(VectorRepoConstants.CONTENT_1, VectorRepoConstants.SOME_SOURCE,
                        VectorRepoConstants.SOURCE_1),
                new PDFText(VectorRepoConstants.CONTENT_2, VectorRepoConstants.SOME_SOURCE,
                        VectorRepoConstants.SOURCE_2));

        // When
        neo4jRepo.save(texts, VectorRepoConstants.TEST_LABEL);
        int countAfterSave = executeCountQuery(VectorRepoConstants.QUERY_COUNT_ALL);

        neo4jRepo.removeText(VectorRepoConstants.TEST_LABEL);
        int countAfterRemove = executeCountQuery(VectorRepoConstants.QUERY_COUNT_ALL);

        // Then
        assertEquals(nodeCount + texts.size(), countAfterSave, "Should add 2 nodes");
        assertEquals(nodeCount, countAfterRemove, "Should remove all added nodes");
    }

    @Test
    public void testRemoveText_withNonExistentLabel() {
        assertThrows(RuntimeException.class, () -> neo4jRepo.removeText(VectorRepoConstants.NON_EXISTENT_LABEL),
                "Should throw RuntimeException when label doesn't exist");

        assertEquals(nodeCount, executeCountQuery(VectorRepoConstants.QUERY_COUNT_ALL),
                "Node count should remain unchanged");
    }

    @Test
    public void testGetConcepts_withExistingConcepts() throws Exception {
        int initialCount = executeCountQuery(VectorRepoConstants.QUERY_COUNT_CONCEPTS);

        executeQuery(String.format(VectorRepoConstants.QUERY_CREATE_CONCEPT, VectorRepoConstants.TEST_CONCEPT_1));
        executeQuery(String.format(VectorRepoConstants.QUERY_CREATE_CONCEPT, VectorRepoConstants.TEST_CONCEPT_2));

        List<Concept> concepts = neo4jRepo.getConcepts();
        assertEquals(initialCount + 2, concepts.size());
        assertEquals(Arrays.asList(VectorRepoConstants.TEST_CONCEPT_1, VectorRepoConstants.TEST_CONCEPT_2), concepts);
    }

    @Test
    public void testAddConcept_withValidData() throws Exception {
        List<SimilarityScore> scores = Arrays.asList(
                new SimilarityScore(new PDFText("Content1", "TestSource", VectorRepoConstants.CONCEPT),
                        VectorRepoConstants.TEST_CONCEPT, 0.9),
                new SimilarityScore(new PDFText("Content2", "TestSource", VectorRepoConstants.CONCEPT),
                        VectorRepoConstants.TEST_CONCEPT, 0.8));

        int initialConceptCount = neo4jRepo.getConcepts().size();
        neo4jRepo.addConcept(scores, VectorRepoConstants.TEST_CONCEPT);

        assertEquals(initialConceptCount + 1, executeCountQuery(VectorRepoConstants.QUERY_COUNT_CONCEPTS));
    }

    @Test
    public void testRemoveConcept_withValidConcept() throws ConceptNotRemovedException, Exception {
        List<SimilarityScore> scores = Arrays.asList(
                new SimilarityScore(
                        new PDFText(VectorRepoConstants.CONTENT_1, "TestSource", VectorRepoConstants.CONCEPT),
                        VectorRepoConstants.TEST_CONCEPT, 0.9),
                new SimilarityScore(
                        new PDFText(VectorRepoConstants.CONTENT_2, "TestSource", VectorRepoConstants.CONCEPT),
                        VectorRepoConstants.TEST_CONCEPT, 0.8));

        int initialConceptCount = neo4jRepo.getConcepts().size();
        neo4jRepo.addConcept(scores, VectorRepoConstants.TEST_CONCEPT);

        assertEquals(initialConceptCount + 1, executeCountQuery(VectorRepoConstants.QUERY_COUNT_CONCEPTS));

        neo4jRepo.removeConcept(VectorRepoConstants.TEST_CONCEPT);

        assertEquals(initialConceptCount,
                executeCountQuery(VectorRepoConstants.QUERY_COUNT_SPECIFIC_CONCEPT, "concept",
                        VectorRepoConstants.TEST_CONCEPT));
    }

    @Test
    public void testRemoveConcept_withNonExistentConcept() throws Exception {
        int initialConceptCount = neo4jRepo.getConcepts().size();

        assertThrows(ConceptNotRemovedException.class,
                () -> neo4jRepo.removeConcept(VectorRepoConstants.NON_EXISTENT_CONCEPT));

        assertEquals(initialConceptCount,
                executeCountQuery(VectorRepoConstants.QUERY_COUNT_SPECIFIC_CONCEPT, "concept",
                        VectorRepoConstants.NON_EXISTENT_CONCEPT));
    }
}
