package com.Orio.gbp_tool.exception;

public class TextAlreadyInTheDatabaseException extends Exception {

    public TextAlreadyInTheDatabaseException(String message) {
        super(message);
    }

    public TextAlreadyInTheDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
