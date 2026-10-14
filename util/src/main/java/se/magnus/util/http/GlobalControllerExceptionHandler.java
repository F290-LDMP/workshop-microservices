package se.magnus.util.http;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.magnus.api.exception.InvalidInputException;
import se.magnus.api.exception.NotFoundException;

@RestControllerAdvice
public class GlobalControllerExceptionHandler {

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<HttpErrorInfo> handleInvalidInputException(HttpServletRequest request, InvalidInputException exception) {
        return createHttpErrorInfo(HttpStatus.UNPROCESSABLE_ENTITY, request, exception.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<HttpErrorInfo> handleNotFoundException(HttpServletRequest request, NotFoundException exception) {
        return createHttpErrorInfo(HttpStatus.NOT_FOUND, request, exception.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<HttpErrorInfo> handleDuplicateKeyException(HttpServletRequest request, DuplicateKeyException exception) {
        return createHttpErrorInfo(HttpStatus.UNPROCESSABLE_ENTITY, request, "Recommendation already exists");
    }

    private ResponseEntity<HttpErrorInfo> createHttpErrorInfo(HttpStatus status, HttpServletRequest request, String message) {
        var errorInfo = new HttpErrorInfo(
                Instant.now(),
                request.getRequestURI(),
                status.value(),
                status.getReasonPhrase(),
                message);
        return ResponseEntity.status(status).body(errorInfo);
    }
}
