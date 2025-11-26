package pwr.zpi.hotspotter.common.exception.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import pwr.zpi.hotspotter.common.exception.ObjectNotFoundException;

import java.nio.file.AccessDeniedException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== Business =====

    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleObjectNotFound(ObjectNotFoundException e) {
        log.debug("Object not found: {}", e.getMessage());
        return buildErrorResponse("Object Not Found", e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException e) {
        log.debug("Illegal argument: {}", e.getMessage());
        return buildErrorResponse("Bad Request", e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalState(IllegalStateException e) {
        log.debug("Illegal state: {}", e.getMessage());
        return buildErrorResponse("Bad Request", e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // ===== Security =====

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(BadCredentialsException e) {
        log.debug("Authentication failed: {}", e.getMessage());
        return buildErrorResponse("Authentication Failed", "Invalid username or password", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException e) {
        log.debug("Access denied: {}", e.getMessage());
        return buildErrorResponse("Access Denied", "No permission to access this resource", HttpStatus.FORBIDDEN);
    }

    // ===== Validation =====

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationErrors(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.debug("Validation failed: {}", message);
        return buildErrorResponse("Validation Failed", message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException e) {
        log.debug("Constraint violation: {}", e.getMessage());
        return buildErrorResponse("Validation Failed", e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.debug("Malformed request: {}", e.getMessage());
        return buildErrorResponse("Malformed Request", "Invalid request body format", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingParameter(MissingServletRequestParameterException e) {
        String message = String.format("Required parameter '%s' is missing", e.getParameterName());
        log.debug("Missing parameter: {}", message);
        return buildErrorResponse("Missing Parameter", message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Parameter '%s' has invalid type", ex.getName());
        log.debug("Type mismatch: {}", ex.getMessage());
        return buildErrorResponse("Invalid Parameter Type", message, HttpStatus.BAD_REQUEST);
    }

    // ===== Database =====

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("Data integrity violation", e);
        String message = extractConstraintViolationMessage(e);
        return buildErrorResponse("Data Integrity Violation", message, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataAccessException(DataAccessException e) {
        log.error("Database access error", e);
        return buildErrorResponse("Database Error", "An error occurred while accessing the database", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ===== Not found =====

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoHandlerFound(NoHandlerFoundException ex) {
        String message = String.format("Endpoint '%s' not found", ex.getRequestURL());
        log.debug("No handler found: {}", message);
        return buildErrorResponse("Endpoint Not Found", message, HttpStatus.NOT_FOUND);
    }

    // ===== Generic =====

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);
        return buildErrorResponse("Internal Server Error", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ===== Helper methods =====

    private ResponseEntity<ErrorResponseDTO> buildErrorResponse(String error, String message, HttpStatus status) {
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(error, message, status.value());
        return ResponseEntity.status(status).body(errorResponse);
    }

    private String extractConstraintViolationMessage(DataIntegrityViolationException ex) {
        String exceptionMessage = ex.getMostSpecificCause().getMessage();

        if (exceptionMessage != null) {
            if (exceptionMessage.contains("unique constraint") || exceptionMessage.contains("Duplicate entry")) {
                return "A record with this information already exists";
            } else if (exceptionMessage.contains("foreign key constraint")) {
                return "Cannot perform operation due to related records";
            } else if (exceptionMessage.contains("not-null constraint")) {
                return "Required field is missing";
            }
        }

        return "Data integrity constraint violated";
    }

}
