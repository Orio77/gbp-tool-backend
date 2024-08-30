package com.Orio.gbp_tool.exception;

public class ChartNotFoundException extends Exception {

    public ChartNotFoundException(String message) {
        super(message);
    }

    public ChartNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
