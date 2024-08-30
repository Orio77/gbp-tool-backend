package com.Orio.gbp_tool.exception;

public class FileAlreadyInTheDatabaseException extends Exception {

    public FileAlreadyInTheDatabaseException(String message) {
        super(message);
    }

    public FileAlreadyInTheDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
