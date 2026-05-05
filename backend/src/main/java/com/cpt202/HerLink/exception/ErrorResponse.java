package com.cpt202.HerLink.exception;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponse {

    private final String code;
    private final int statusCode;
    private final String message;
    private final List<String> details;
    private final String path;
    private final LocalDateTime timestamp;

    public ErrorResponse(ErrorCode code, String message, List<String> details, String path, LocalDateTime timestamp) {
        this.code = code.name();
        this.statusCode = code.getStatusCode();
        this.message = message;
        this.details = details;
        this.path = path;
        this.timestamp = timestamp;
    }

    public String getCode() {
        return code;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getDetails() {
        return details;
    }

    public String getPath() {
        return path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
