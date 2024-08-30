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
import com.Orio.gbp_tool.repository.IGraphDatabaseRepo;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class Neo4jRepo implements IGraphDatabaseRepo {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jRepo.class);
    private final Driver driver;

    @Override
    public void save(List<PDFText> texts, String label) throws TextAlreadyInTheDatabaseException {
        logger.info("Starting save method with label: {}", label);
        Assert.notNull(texts, "Provided texts cannot be empty");
        Assert.hasText(label, "Provided label cannot be empty");

        if (existsTextWithLabel(label)) {
            throw new TextAlreadyInTheDatabaseException(
                    String.format("Text with the provided label \"%s\" already exists in the database", label));
        }

        String cypherQuery = "UNWIND $texts AS text " +
                "CREATE (n:TextNode {content: text.content, name: text.name})";
        List<Map<String, Object>> params = new ArrayList<>();
        for (PDFText text : texts) {
            Map<String, Object> param = new HashMap<>();
            param.put("content", text.getText());
            param.put("name", text.getLabel());
            params.add(param);
        }
        logger.debug("Cypher query: {}", cypherQuery);
        logger.debug("Parameters: {}", params);
        try (Session session = driver.session()) {
            session.run(cypherQuery, Values.parameters("texts", params));
            logger.info("Save method executed successfully");
        } catch (Exception e) {
            logger.error("Error executing save method", e);
        }
    }

    private boolean existsTextWithLabel(String label) {
        logger.info("Starting existsTextWithLabel method with label: {}", label);
        String cypherQuery = "MATCH (n:TextNode) WHERE n.name CONTAINS $label RETURN n LIMIT 1";
        logger.debug("Cypher query: {}", cypherQuery);
        logger.debug("Label: {}", label);
        try (Session session = driver.session()) {
            Result result = session.run(cypherQuery, Values.parameters("label", label));
            boolean exists = result.hasNext();
            logger.info("existsTextWithLabel method executed successfully, exists: {}", exists);
            return exists;
        } catch (Exception e) {
            logger.error("Error executing existsTextWithLabel method", e);
            return false;
        }
    }

    @Override
    public void removeText(String label) {
        logger.info("Starting removeText method with label: {}", label);
        Assert.hasText(label, "Provided label cannot be empty");
        String cypherQuery = "MATCH (n:TextNode) WHERE n.name CONTAINS $label DELETE n";
        logger.debug("Cypher query: {}", cypherQuery);
        logger.debug("Label: {}", label);
        try (Session session = driver.session()) {
            session.run(cypherQuery, Values.parameters("label", label));
            logger.info("removeText method executed successfully");
        } catch (Exception e) {
            logger.error("Error executing removeText method", e);
        }
    }

    @Override
    public List<String> getConcepts() throws Exception {
        logger.info("Starting getConcepts method");
        String cypherQuery = "MATCH (c:Concept) RETURN c.name AS name";
        List<String> concepts = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(cypherQuery); // Corrected type
            concepts = result.stream()
                    .map(record -> record.get("name").asString())
                    .toList(); // Corrected method to collect results
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
        Assert.notNull(scores, "Provided scores cannot be empty");
        Assert.hasText(concept, "Provided concept cannot be empty");

        String cypherQuery = "CREATE (c:Concept {name: $concept}) " +
                "WITH c " +
                "UNWIND $scores AS score " +
                "CREATE (n:TextNode {content: score.content, name: score.name}) " +
                "MERGE (c)-[:SIMILARITY {score: score.score}]->(n)";

        List<Map<String, Object>> params = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            Map<String, Object> param = new HashMap<>();
            param.put("content", scores.get(i).getText().getText());
            param.put("name", scores.get(i).getText().getSource() + i);
            param.put("score", scores.get(i).getScore());
            params.add(param);
        }

        logger.debug("Cypher query: {}", cypherQuery);
        logger.debug("Parameters: {}", params);

        try (Session session = driver.session()) {
            session.run(cypherQuery, Values.parameters("concept", concept, "scores", params));
            logger.info("addConcept method executed successfully");
        } catch (Exception e) {
            logger.error("Error executing addConcept method", e);
        }
    }

    @Override
    public boolean removeConcept(String concept) throws ConceptNotRemovedException {
        logger.info("Starting removeConcept method with concept: {}", concept);
        Assert.hasText(concept, "Provided concept cannot be empty");

        String countQuery = "MATCH (c:Concept) RETURN count(c) AS count";
        String deleteQuery = "MATCH (c:Concept {name: $concept}) DETACH DELETE c";

        int countBefore = -1;
        int countAfter = -1;

        try (Session session = driver.session()) {
            // Count before deletion
            countBefore = session.run(countQuery).single().get("count").asInt();
            logger.info("Count of concept nodes before deletion: {}", countBefore);

            // Perform deletion
            session.run(deleteQuery, Values.parameters("concept", concept));
            logger.info("Concept node with name '{}' deleted successfully", concept);

            // Count after deletion
            countAfter = session.run(countQuery).single().get("count").asInt();
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
                    countBefore, countAfter));
        }
    }

}
