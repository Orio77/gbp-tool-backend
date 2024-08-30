package com.Orio.gbp_tool.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
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

import com.Orio.gbp_tool.exception.ConceptNotRemovedException;
import com.Orio.gbp_tool.exception.TextAlreadyInTheDatabaseException;
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

    @BeforeEach
    public void setUp() {
        try (Session session = driver.session()) {
            Result result = session.run("MATCH (n:TextNode) RETURN count(n) AS count");
            int count = result.single().get("count").asInt();
            nodeCount = count;
        }
    }

    @AfterEach
    public void tearDown() {
        try (Session session = driver.session()) {
            session.run("MATCH (n) WHERE n.name CONTAINS 'TestLabel' DETACH DELETE n");
            session.run("MATCH (c) WHERE c.name CONTAINS 'TestConcept' DETACH DELETE c");
        }
    }

    @Test
    public void testSave_withValidData() throws TextAlreadyInTheDatabaseException {
        List<PDFText> texts = Arrays.asList(
                new PDFText("Content 1", "Some source", "source1"),
                new PDFText("Content 2", "Some source", "source2"));
        String label = "TestLabel";

        neo4jRepo.save(texts, label);

        try (Session session = driver.session()) {
            Result result = session.run("MATCH (n:TextNode) RETURN count(n) AS count");
            int count = result.single().get("count").asInt();
            assertEquals(nodeCount + 2, count);
        }
    }

    @Test
    public void testSave_withEmptyList() throws TextAlreadyInTheDatabaseException {
        List<PDFText> texts = Arrays.asList();
        String label = "TestLabel";

        neo4jRepo.save(texts, label);

        try (Session session = driver.session()) {
            Result result = session.run("MATCH (n:TextNode) RETURN count(n) AS count");
            int count = result.single().get("count").asInt();
            assertEquals(nodeCount, count);
        }
    }

    @Test
    public void testRemoveText_withValidLabel() throws TextAlreadyInTheDatabaseException {
        List<PDFText> texts = Arrays.asList(
                new PDFText("Content 1", "Some source", "source1"),
                new PDFText("Content 2", "Some source", "source2"));
        String label = "TestLabel";

        neo4jRepo.save(texts, label);

        try (Session session = driver.session()) {
            Result result = session.run("MATCH (n:TextNode) RETURN count(n) AS count");
            int count = result.single().get("count").asInt();
            assertEquals(nodeCount + 2, count);
        }

        neo4jRepo.removeText(label);

        try (Session session = driver.session()) {
            Result result = session.run("MATCH (n:TextNode) RETURN count(n) AS count");
            int count = result.single().get("count").asInt();
            assertEquals(nodeCount, count);
        }
    }

    @Test
    public void testRemoveText_withNonExistentLabel() {
        String label = "NonExistentLabel";

        neo4jRepo.removeText(label);

        try (Session session = driver.session()) {
            Result result = session.run("MATCH (n:TextNode) RETURN count(n) AS count");
            int count = result.single().get("count").asInt();
            assertEquals(nodeCount, count);
        }
    }

    @Test
    public void testGetConcepts_withExistingConcepts() throws Exception {
        int initialCount;
        try (Session session = driver.session()) {
            initialCount = session.run("MATCH (c:Concept) RETURN count(c) AS count")
                    .single()
                    .get("count")
                    .asInt();
            session.run("CREATE (c:Concept {name: 'TestConcept1'})");
            session.run("CREATE (c:Concept {name: 'TestConcept2'})");
        }

        List<String> concepts = neo4jRepo.getConcepts();
        assertEquals(initialCount + 2, concepts.size());
        assertEquals(Arrays.asList("TestConcept1", "TestConcept2"), concepts);
    }

    @Test
    public void testAddConcept_withValidData() throws Exception {
        String concept = "TestConcept";
        List<SimilarityScore> scores = Arrays.asList(
                new SimilarityScore(new PDFText("Content1", "TestLabel1", "source1"), concept, 0.9),
                new SimilarityScore(new PDFText("Content2", "TestLabel2", "source2"), concept, 0.8));

        int initialConceptCount = neo4jRepo.getConcepts().size();
        neo4jRepo.addConcept(scores, concept);

        try (Session session = driver.session()) {
            Result result = session.run(
                    "MATCH (c:Concept) RETURN count(c) AS count",
                    Values.parameters("concept", concept));
            int count = result.single().get("count").asInt();
            assertEquals(initialConceptCount + 1, count);
        }
    }

    @Test
    public void testRemoveConcept_withValidConcept() throws ConceptNotRemovedException, Exception {
        String concept = "TestConcept";
        List<SimilarityScore> scores = Arrays.asList(
                new SimilarityScore(new PDFText("Content 1", "TestLabel1", "source1"), concept, 0.9),
                new SimilarityScore(new PDFText("Content 2", "TestLabel2", "source2"), concept, 0.8));

        int initialConceptCount = neo4jRepo.getConcepts().size();
        neo4jRepo.addConcept(scores, concept);

        try (Session session = driver.session()) {
            Result result = session.run(
                    "MATCH (c:Concept) RETURN count(c) AS count",
                    Values.parameters("concept", concept));
            int count = result.single().get("count").asInt();
            assertEquals(initialConceptCount + 1, count);
        }

        neo4jRepo.removeConcept(concept);

        try (Session session = driver.session()) {
            Result result = session.run("MATCH (c:Concept {name: $concept}) RETURN count(c) AS count",
                    Values.parameters("concept", concept));
            int count = result.single().get("count").asInt();
            assertEquals(initialConceptCount, count);
        }
    }

    @Test
    public void testRemoveConcept_withNonExistentConcept() throws Exception {
        String concept = "NonExistentConcept";

        int initialConceptCount = neo4jRepo.getConcepts().size();
        try {
            neo4jRepo.removeConcept(concept);
        } catch (ConceptNotRemovedException e) {
            // Concept should not be removed because it does not exist
        }

        try (Session session = driver.session()) {
            Result result = session.run("MATCH (c:Concept {name: $concept}) RETURN count(c) AS count",
                    Values.parameters("concept", concept));
            int count = result.single().get("count").asInt();
            assertEquals(initialConceptCount, count);
        }
    }

}