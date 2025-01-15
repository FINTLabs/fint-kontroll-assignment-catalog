package no.fintlabs.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        log.error("An unhandled exception occurred with message: {}", ex.getMessage(), ex);

        if (ex instanceof ResponseStatusException) {
            return ResponseEntity.status(((ResponseStatusException) ex).getStatusCode()).body(ex.getMessage());
        }

        return ResponseEntity.status(500).body("An unexpected error occurred: " + ex.getMessage());
    }
}
