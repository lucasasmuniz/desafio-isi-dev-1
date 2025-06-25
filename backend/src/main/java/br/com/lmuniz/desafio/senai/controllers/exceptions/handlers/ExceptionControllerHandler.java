package br.com.lmuniz.desafio.senai.controllers.exceptions.handlers;

import br.com.lmuniz.desafio.senai.controllers.exceptions.FieldMessage;
import br.com.lmuniz.desafio.senai.controllers.exceptions.StandardException;
import br.com.lmuniz.desafio.senai.controllers.exceptions.ValidationException;
import br.com.lmuniz.desafio.senai.services.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ExceptionControllerHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardException> entityNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        StandardException error = new StandardException();
        HttpStatus status = HttpStatus.NOT_FOUND;
        error.setTimestamp(Instant.now());
        error.setStatus(status.value());
        error.setError("Resource not found exception");
        error.setMessage(e.getMessage());
        error.setPath(request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<StandardException> entityConflict(ResourceConflictException e, HttpServletRequest request) {
        StandardException error = new StandardException();
        HttpStatus status = HttpStatus.CONFLICT;
        error.setTimestamp(Instant.now());
        error.setStatus(status.value());
        error.setError("Resource conflict exception");
        error.setMessage(e.getMessage());
        error.setPath(request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ValidationException> businessRuleException(BusinessRuleException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ValidationException err = new ValidationException();
        err.setTimestamp(Instant.now());
        err.setStatus(status.value());
        err.setPath(request.getRequestURI());
        if (e.getErrors() != null && !e.getErrors().isEmpty()) {
            err.setError("Validation error");
            err.setMessage("One or more fields are invalid. Please check the 'errors' list.");
            for (Map.Entry<String, String> entry : e.getErrors().entrySet()) {
                err.getErrors().add(new FieldMessage(entry.getKey(), entry.getValue()));
            }
        }
        else {
            err.setError("Business rule exception");
            err.setMessage(e.getMessage());
            err.getErrors().add(new FieldMessage("error", e.getMessage()));
        }
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(InvalidPriceException.class)
    public ResponseEntity<StandardException> InvalidPriceException(InvalidPriceException e, HttpServletRequest request) {
        StandardException error = new StandardException();
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        error.setTimestamp(Instant.now());
        error.setStatus(status.value());
        error.setError("Invalid price exception");
        error.setMessage(e.getMessage());
        error.setPath(request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<StandardException> DatabaseException(DatabaseException e, HttpServletRequest request) {
        StandardException error = new StandardException();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        error.setTimestamp(Instant.now());
        error.setStatus(status.value());
        error.setError("An internal data consistency error occurred.");
        error.setMessage(e.getMessage());
        error.setPath(request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationException> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ValidationException err = new ValidationException();
        err.setTimestamp(Instant.now());
        err.setStatus(status.value());
        err.setError("Validation exception");
        err.setMessage(e.getMessage());
        err.setPath(request.getRequestURI());
        for(FieldError f: e.getBindingResult().getFieldErrors()) {
            err.getErrors().add(new FieldMessage(f.getField(), f.getDefaultMessage()));
        }
        return ResponseEntity.status(status).body(err);
    }
}
