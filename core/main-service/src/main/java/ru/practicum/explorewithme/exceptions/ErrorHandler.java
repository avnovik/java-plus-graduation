package ru.practicum.explorewithme.exceptions;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.practicum.explorewithme.exceptions.dto.ApiError;

import java.time.LocalDateTime;
import java.util.Collections;


@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            MissingServletRequestParameterException.class, HttpMessageNotReadableException.class,
            IllegalArgumentException.class, HandlerMethodValidationException.class, ValidationException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        log.warn("Некорректный запрос: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        log.warn("Объект не найден: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND,
                "The required object was not found.",
                ex.getMessage());
    }

    @ExceptionHandler({
            ConditionsNotMetException.class,
            ConflictException.class
    })
    public ResponseEntity<ApiError> handleConditionsNotMet(RuntimeException ex) {
        log.warn("Нарушены условия операции: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT,
                "For the requested operation the conditions are not met.",
                ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Нарушение целостности данных: {}",
                ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT,
                "Integrity constraint has been violated.",
                ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiError> handleOther(Throwable ex) {
        log.warn("Непредвиденная ошибка: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error.",
                ex.getMessage());
    }

    private ResponseEntity<ApiError> buildErrorResponse(HttpStatus status, String reason, String message) {
        ApiError body = ApiError.builder()
                .errors(Collections.emptyList())
                .status(status.name())
                .reason(reason)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
