package com.Orio.gbp_tool.exception;

public class ConceptNotRemovedException extends Exception {

    public ConceptNotRemovedException(String message) {
        super(message);
    }

    public ConceptNotRemovedException(String message, Throwable cause) {
        super(message, cause);
    }
}
