package br.com.fatecararas.util.http;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import br.com.fatecararas.api.exceptions.InvalidInputException;
import br.com.fatecararas.api.exceptions.NotFoundException;

@RestControllerAdvice
public class GlobalControllerExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<HttpErrorInfo> notFound(
            NotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, request, ex.getMessage());
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<HttpErrorInfo> invalidInput(
            InvalidInputException ex, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessage());
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<HttpErrorInfo> badRequest(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, request,
                "Requisição inválida: confira o JSON e os parâmetros obrigatórios");
    }

    private ResponseEntity<HttpErrorInfo> error(
            HttpStatus status, HttpServletRequest request, String message) {
        return ResponseEntity.status(status)
                .body(new HttpErrorInfo(status, request.getRequestURI(), message));
    }
}
