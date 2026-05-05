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
                .body(buildErrorResponse(exception.getErrorCode(), exception.getMessage(), exception.getDetails(), request));
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            ServletRequestBindingException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(ErrorCode.BAD_REQUEST, exception.getMessage(), List.of(), request));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildErrorResponse(ErrorCode.CONFLICT, exception.getMessage(), List.of(), request));
    }

    @ExceptionHandler({
            MultipartException.class,
            MaxUploadSizeExceededException.class
    })
    public ResponseEntity<ErrorResponse> handleFileUploadException(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(ErrorCode.FILE_UPLOAD_ERROR, exception.getMessage(), List.of(), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(ErrorCode.INTERNAL_ERROR, "Internal server error.", List.of(), request));
    }

    private ErrorResponse buildErrorResponse(ErrorCode errorCode, String message, List<String> details, HttpServletRequest request) {
        return new ErrorResponse(errorCode, message, details, request.getRequestURI(), LocalDateTime.now());
    }
}
