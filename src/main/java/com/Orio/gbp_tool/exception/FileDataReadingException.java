package com.Orio.gbp_tool.exception;

public class FileDataReadingException extends Exception {

    public FileDataReadingException(String message) {
        super(message);
    }

    public FileDataReadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
