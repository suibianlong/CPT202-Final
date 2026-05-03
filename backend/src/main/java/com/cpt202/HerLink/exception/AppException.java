package com.cpt202.HerLink.exception;

import java.util.Collections;
import java.util.List;

// custom application exception
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final int statusCode;
    private final List<String> details;

    public AppException(int statusCode, String message) {
        this(ErrorCode.fromStatusCode(statusCode), message, Collections.emptyList());
    }

    public AppException(int statusCode, String message, List<String> details) {
        this(ErrorCode.fromStatusCode(statusCode), message, details);
    }

    public AppException(ErrorCode errorCode, String message) {
        this(errorCode, message, Collections.emptyList());
    }

    public AppException(ErrorCode errorCode, String message, List<String> details) {
        super(message);
        this.errorCode = errorCode == null ? ErrorCode.INTERNAL_ERROR : errorCode;
        this.statusCode = this.errorCode.getStatusCode();
        this.details = details == null ? Collections.emptyList() : details;
    }

    public static AppException badRequest(String message) {
        return new AppException(ErrorCode.BAD_REQUEST, message);
    }

    public static AppException badRequest(String message, List<String> details) {
        return new AppException(ErrorCode.BAD_REQUEST, message, details);
    }

    public static AppException unauthorized(String message) {
        return new AppException(ErrorCode.UNAUTHORIZED, message);
    }

    public static AppException forbidden(String message) {
        return new AppException(ErrorCode.FORBIDDEN, message);
    }

    public static AppException notFound(String message) {
        return new AppException(ErrorCode.NOT_FOUND, message);
    }

    public static AppException conflict(String message) {
        return new AppException(ErrorCode.CONFLICT, message);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public List<String> getDetails() {
        return details;
    }
}
