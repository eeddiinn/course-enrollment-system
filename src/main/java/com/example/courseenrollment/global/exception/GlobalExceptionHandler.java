package com.example.courseenrollment.global.exception;

import com.example.courseenrollment.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

import static com.example.courseenrollment.global.exception.ErrorType.INTERNAL_SERVER_ERROR;
import static com.example.courseenrollment.global.exception.ErrorType.REQUEST_VALIDATION_EXCEPTION;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<?>> handleValidationException(
        MethodArgumentNotValidException e
    ) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity
                   .status(REQUEST_VALIDATION_EXCEPTION.getHttpStatus())
                   .body(ApiResponse.error(REQUEST_VALIDATION_EXCEPTION, errors));
    }

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ApiResponse<?>> handleCustomException(CustomException e) {
        ErrorType errorType = e.getErrorType();

        return ResponseEntity
                   .status(errorType.getHttpStatus())
                   .body(ApiResponse.error(errorType));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("[Unhandled Exception]", e);

        return ResponseEntity
                   .status(INTERNAL_SERVER_ERROR.getHttpStatus())
                   .body(ApiResponse.error(INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException e
    ) {
        return ResponseEntity
                   .status(ErrorType.INVALID_COURSE_STATUS.getHttpStatus())
                   .body(ApiResponse.error(ErrorType.INVALID_COURSE_STATUS));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException e
    ) {
        return ResponseEntity
                   .status(ErrorType.INVALID_COURSE_STATUS.getHttpStatus())
                   .body(ApiResponse.error(ErrorType.INVALID_COURSE_STATUS));
    }
}
