package com.cpt202.HerLink.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(buildErrorResponse(exception.getStatusCode(), exception.getMessage(), exception.getDetails(), request));
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            ServletRequestBindingException.class,
            MultipartException.class,
            MaxUploadSizeExceededException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(400, exception.getMessage(), List.of(), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(500, "Internal server error.", List.of(), request));
    }

    private ErrorResponse buildErrorResponse(int statusCode, String message, List<String> details, HttpServletRequest request) {
        return new ErrorResponse(statusCode, message, details, request.getRequestURI(), LocalDateTime.now());
    }

    public static class ErrorResponse {

        private final int statusCode;
        private final String message;
        private final List<String> details;
        private final String path;
        private final LocalDateTime timestamp;

        public ErrorResponse(int statusCode, String message, List<String> details, String path, LocalDateTime timestamp) {
            this.statusCode = statusCode;
            this.message = message;
            this.details = details;
            this.path = path;
            this.timestamp = timestamp;
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
}
