package com.Orio.gbp_tool.repository.impl.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import com.Orio.gbp_tool.exception.ConceptNotRemovedException;
import com.Orio.gbp_tool.exception.TextAlreadyInTheDatabaseException;
import com.Orio.gbp_tool.model.PDFText;
import com.Orio.gbp_tool.model.SimilarityScore;
import com.Orio.gbp_tool.model.Concept;
import com.Orio.gbp_tool.repository.IGraphDatabaseRepo;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class Neo4jRepo implements IGraphDatabaseRepo {

    // Constants for validation messages
    private static final String TEXTS_NOT_NULL_MESSAGE = "Provided texts cannot be null";
    private static final String TEXTS_NOT_EMPTY_MESSAGE = "Provided texts cannot be empty";
    private static final String LABEL_NOT_EMPTY_MESSAGE = "Provided label cannot be empty";

    // Constants for exception messages
    private static final String TEXT_ALREADY_IN_DATABASE_MESSAGE = "Text with the provided label \"%s\" already exists in the database";
    private static final String TEXT_SAVE_FAILURE_MESSAGE = "Failed to save texts to database";
    private static final String TEXT_EXISTENCE_CHECK_FAILURE_MESSAGE = "Failed to check if text exists";
    private static final String TEXT_REMOVAL_FAILURE_MESSAGE = "Failed to remove text from database";
    private static final String NO_NODES_FOUND_MESSAGE = "No nodes found with label: %s";

    // Constants for Cypher queries
    private static final String SAVE_CYPHER_QUERY_TEMPLATE = "UNWIND $texts AS text CREATE (n:%s {content: text.content, name: text.name})";
    private static final String EXISTS_TEXT_CYPHER_QUERY_TEMPLATE = "MATCH (n:%s) RETURN n LIMIT 1";
    private static final String REMOVE_TEXT_CYPHER_QUERY_TEMPLATE = "MATCH (n:%s) DELETE n";
    private static final String GET_CONCEPTS_CYPHER_QUERY = "MATCH (c:Concept)-[r:SIMILARITY]->(t:TextNode) " +
            "RETURN c.name as name, collect(t.name) as associatedTexts";
    private static final String ADD_CONCEPT_CYPHER_QUERY = "MERGE (c:Concept {name: $concept, text: $concept}) " +
            "WITH c " +
            "UNWIND $scores as score " +
            "MERGE (n:TextNode {content: score.content}) " +
            "ON CREATE SET n.name = score.name " +
            "MERGE (c)-[r:SIMILARITY]->(n) " +
            "ON CREATE SET r.score = score.score " +
            "ON MATCH SET r.score = score.score";
    private static final String COUNT_CONCEPTS_CYPHER_QUERY = "MATCH (c:Concept) RETURN count(c) AS count";
    private static final String DELETE_CONCEPT_CYPHER_QUERY = "MATCH (c:Concept {name: $concept}) DETACH DELETE c";

    private static final Logger logger = LoggerFactory.getLogger(Neo4jRepo.class);
    private final Driver driver;

    @Override
    public void save(List<PDFText> texts, String label) throws TextAlreadyInTheDatabaseException {
        logger.info("Starting save method with label: {}", label);
        validateSaveInputs(texts, label);

        if (existsTextWithLabel(label)) {
            throw new TextAlreadyInTheDatabaseException(
                    String.format(TEXT_ALREADY_IN_DATABASE_MESSAGE, label));
        }

        String cypherQuery = String.format(SAVE_CYPHER_QUERY_TEMPLATE, label);
        List<Map<String, Object>> params = prepareSaveParameters(texts);

        logger.debug("Cypher query: {}", cypherQuery);
        logger.debug("Parameters: {}", params);

        try (Session session = driver.session()) {
            executeWriteTransaction(session, cypherQuery, "texts", params);
            logger.info("Save method executed successfully");
        } catch (Exception e) {
            logger.error("Error executing save method", e);
            throw new RuntimeException(TEXT_SAVE_FAILURE_MESSAGE, e);
        }
    }

    @Override
    public void removeText(String label) {
        logger.info("Starting removeText method with label: {}", label);
        Assert.hasText(label, LABEL_NOT_EMPTY_MESSAGE);

        String cypherQuery = String.format(REMOVE_TEXT_CYPHER_QUERY_TEMPLATE, label);

        logger.debug("Cypher query: {}", cypherQuery);
        logger.debug("Label: {}", label);

        try (Session session = driver.session()) {
            long deletedCount = session.executeWrite(tx -> {
                Result result = tx.run(cypherQuery);
                return (long) result.consume().counters().nodesDeleted();
            });

            logger.info("Deleted {} nodes with label {}", deletedCount, label);

            if (deletedCount == 0) {
                throw new RuntimeException(String.format(NO_NODES_FOUND_MESSAGE, label));
            }
        } catch (Exception e) {
            logger.error("Error executing removeText method", e);
            throw new RuntimeException(TEXT_REMOVAL_FAILURE_MESSAGE, e);
        }
    }

    @Override
    public List<Concept> getConcepts() throws Exception {
        logger.info("Starting getConcepts method");
        List<Concept> concepts = new ArrayList<>();

        try (Session session = driver.session()) {
            Result result = session.run(GET_CONCEPTS_CYPHER_QUERY);

            while (result.hasNext()) {
                var record = result.next();
                String name = record.get("name").asString();
                List<String> associatedTexts = record.get("associatedTexts").asList(Values.ofString());
                concepts.add(new Concept(name, associatedTexts));
            }

            logger.info("getConcepts method executed successfully");
        } catch (Exception e) {
            logger.error("Error executing getConcepts method", e);
            throw new Exception(e.getMessage(), e);
        }

        return concepts;
    }

    @Override
    public void addConcept(List<SimilarityScore> scores, String concept) {
        logger.info("Starting addConcept method with concept: {}", concept);
        Assert.notNull(scores, "Provided scores cannot be null");
        Assert.notEmpty(scores, "Provided scores cannot be empty");
        Assert.hasText(concept, "Provided concept cannot be empty");

        String cypherQuery = ADD_CONCEPT_CYPHER_QUERY;
        List<Map<String, Object>> params = prepareAddConceptParameters(scores);

        logger.debug("Cypher query: {}", cypherQuery);
        logger.debug("Parameters: concept={}, scores={}", concept, params);

        try (Session session = driver.session()) {
            session.run(cypherQuery, Values.parameters("concept", concept, "scores", params));
            logger.info("addConcept method executed successfully");
        } catch (Exception e) {
            logger.error("Error executing addConcept method", e);
            throw new RuntimeException("Failed to add concept to database", e);
        }
    }

    @Override
    public boolean removeConcept(String concept) throws ConceptNotRemovedException {
        logger.info("Starting removeConcept method with concept: {}", concept);
        Assert.hasText(concept, LABEL_NOT_EMPTY_MESSAGE);

        int countBefore = -1;
        int countAfter = -1;

        try (Session session = driver.session()) {
            // Count before deletion
            countBefore = getConceptCount(session);
            logger.info("Count of concept nodes before deletion: {}", countBefore);

            // Perform deletion
            session.run(DELETE_CONCEPT_CYPHER_QUERY, Values.parameters("concept", concept));
            logger.info("Concept node with name '{}' deleted successfully", concept);

            // Count after deletion
            countAfter = getConceptCount(session);
            logger.info("Count of concept nodes after deletion: {}", countAfter);

            // Verify decrement
            if (countAfter == countBefore - 1) {
                logger.info("Concept node count decremented successfully");
                return true;
            } else {
                logger.warn("Concept node count did not decrement as expected");
                throw new ConceptNotRemovedException(String.format(
                        "Failed to remove concept. Initial number of concepts was: %d. Number of concepts after failed removal is: %d",
                        countBefore, countAfter));
            }
        } catch (Exception e) {
            logger.error("Error executing removeConcept method", e);
            throw new ConceptNotRemovedException(String.format(
                    "Failed to remove concept. Initial number of concepts was: %d. Number of concepts after failed removal is: %d",
                    countBefore, countAfter), e);
        }
    }

    public boolean existsTextWithLabel(String label) {
        logger.info("Starting existsTextWithLabel method with label: {}", label);
        String cypherQuery = String.format(EXISTS_TEXT_CYPHER_QUERY_TEMPLATE, label);
        logger.debug("Cypher query: {}", cypherQuery);

        try (Session session = driver.session()) {
            Result result = session.run(cypherQuery);
            boolean exists = result.hasNext();
            logger.info("existsTextWithLabel method executed successfully, exists: {}", exists);
            return exists;
        } catch (Exception e) {
            logger.error("Error executing existsTextWithLabel method", e);
            throw new RuntimeException(TEXT_EXISTENCE_CHECK_FAILURE_MESSAGE, e);
        }
    }

    // Private Helper Methods

    /**
     * Validates the inputs for the save method.
     *
     * @param texts List of PDFText objects.
     * @param label The label string.
     */
    private void validateSaveInputs(List<PDFText> texts, String label) {
        Assert.notNull(texts, TEXTS_NOT_NULL_MESSAGE);
        Assert.notEmpty(texts, TEXTS_NOT_EMPTY_MESSAGE);
        Assert.hasText(label, LABEL_NOT_EMPTY_MESSAGE);
    }

    /**
     * Prepares the parameters for the save Cypher query.
     *
     * @param texts List of PDFText objects.
     * @return A list of parameter maps.
     */
    private List<Map<String, Object>> prepareSaveParameters(List<PDFText> texts) {
        List<Map<String, Object>> params = new ArrayList<>();
        for (PDFText text : texts) {
            Map<String, Object> param = new HashMap<>();
            param.put("content", text.getText());
            param.put("name", text.getLabel());
            params.add(param);
        }
        return params;
    }

    /**
     * Prepares the parameters for the addConcept Cypher query.
     *
     * @param scores List of SimilarityScore objects.
     * @return A list of parameter maps.
     */
    private List<Map<String, Object>> prepareAddConceptParameters(List<SimilarityScore> scores) {
        List<Map<String, Object>> params = new ArrayList<>();
        for (SimilarityScore score : scores) {
            Map<String, Object> map = new HashMap<>();
            map.put("content", score.getText().getText());
            map.put("name", score.getText().getSource());
            map.put("score", score.getScore());
            params.add(map);
        }
        return params;
    }

    /**
     * Executes a write transaction without returning a result.
     *
     * @param session     The Neo4j session.
     * @param cypherQuery The Cypher query string.
     * @param paramKey    The parameter key.
     * @param paramValue  The parameter value.
     */
    private void executeWriteTransaction(Session session, String cypherQuery, String paramKey, Object paramValue) {
        session.executeWrite(tx -> {
            if (paramKey != null && paramValue != null) {
                tx.run(cypherQuery, Values.parameters(paramKey, paramValue));
            } else {
                tx.run(cypherQuery);
            }
            return null;
        });
    }

    /**
     * Retrieves the current count of Concept nodes in the database.
     *
     * @param session The Neo4j session.
     * @return The count of Concept nodes.
     */
    private int getConceptCount(Session session) {
        Result result = session.run(COUNT_CONCEPTS_CYPHER_QUERY);
        return result.single().get("count").asInt();
    }
}
