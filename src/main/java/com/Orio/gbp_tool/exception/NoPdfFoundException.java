package com.Orio.gbp_tool.exception;

public class NoPdfFoundException extends Exception {

    public NoPdfFoundException(String message) {
        super(message);
    }

    public NoPdfFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
