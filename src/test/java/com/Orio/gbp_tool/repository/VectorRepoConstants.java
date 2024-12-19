package com.Orio.gbp_tool.repository;

/**
 * A utility class that holds all test-related constants.
 */
public class VectorRepoConstants {
    // Label Constants
    public static final String TEST_LABEL = "TestLabel";
    public static final String CONCEPT = "Concept";

    // Content Constants
    public static final String CONTENT_1 = "Content 1";
    public static final String CONTENT_2 = "Content 2";
    public static final String SOME_SOURCE = "Some source";
    public static final String SOURCE_1 = "source1";
    public static final String SOURCE_2 = "source2";

    // Concept Constants
    public static final String TEST_CONCEPT_1 = "TestConcept1";
    public static final String TEST_CONCEPT_2 = "TestConcept2";
    public static final String TEST_CONCEPT = "TestConcept";

    // Non-Existent Constants
    public static final String NON_EXISTENT_LABEL = "NonExistentLabel";
    public static final String NON_EXISTENT_CONCEPT = "NonExistentConcept";

    // Query Constants
    public static final String QUERY_COUNT_ALL = "MATCH (n) RETURN count(n) AS count";
    public static final String QUERY_COUNT_CONCEPTS = "MATCH (c:Concept) RETURN count(c) AS count";
    public static final String QUERY_DELETE_TEST_LABEL = "MATCH (n:%s) DELETE n";
    public static final String QUERY_DELETE_CONCEPT = "MATCH (c:Concept {name: $concept}) DETACH DELETE c";
    public static final String QUERY_DELETE_CONTAINING_NAME = "MATCH (n) WHERE n.name CONTAINS $name DETACH DELETE n";
    public static final String QUERY_CREATE_CONCEPT = "CREATE (:Concept {name: '%s'})";
    public static final String QUERY_COUNT_SPECIFIC_CONCEPT = "MATCH (c:Concept {name: $concept}) RETURN count(c) AS count";
}
