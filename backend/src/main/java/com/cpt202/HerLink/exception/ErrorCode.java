package com.cpt202.HerLink.exception;

public enum ErrorCode {
    SUCCESS(200),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    FILE_UPLOAD_ERROR(400),
    INTERNAL_ERROR(500);

    private final int statusCode;

    ErrorCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public static ErrorCode fromStatusCode(int statusCode) {
        return switch (statusCode) {
            case 400 -> BAD_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 409 -> CONFLICT;
            case 500 -> INTERNAL_ERROR;
            default -> INTERNAL_ERROR;
        };
    }
}
