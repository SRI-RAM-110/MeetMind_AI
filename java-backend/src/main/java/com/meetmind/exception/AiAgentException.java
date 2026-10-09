package com.meetmind.exception;

public class AiAgentException extends RuntimeException {

    private final int statusCode;

    public AiAgentException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public AiAgentException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
