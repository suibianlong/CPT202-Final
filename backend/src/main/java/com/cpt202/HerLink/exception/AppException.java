package com.cpt202.HerLink.exception;

import java.util.Collections;
import java.util.List;

// custom application exception
public class AppException extends RuntimeException {

    private final int statusCode;
    private final List<String> details;

    public AppException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.details = Collections.emptyList();
    }

    public AppException(int statusCode, String message, List<String> details) {
        super(message);
        this.statusCode = statusCode;
        this.details = details == null ? Collections.emptyList() : details;
    }

    public static AppException badRequest(String message) {
        return new AppException(400, message);
    }

    public static AppException badRequest(String message, List<String> details) {
        return new AppException(400, message, details);
    }

    public static AppException unauthorized(String message) {
        return new AppException(401, message);
    }

    public static AppException forbidden(String message) {
        return new AppException(403, message);
    }

    public static AppException notFound(String message) {
        return new AppException(404, message);
    }

    public static AppException conflict(String message) {
        return new AppException(409, message);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public List<String> getDetails() {
        return details;
    }
}
