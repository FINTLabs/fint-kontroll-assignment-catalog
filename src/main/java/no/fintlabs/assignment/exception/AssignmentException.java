package no.fintlabs.assignment.exception;

import no.fintlabs.exception.KontrollException;
import org.springframework.http.HttpStatus;

public class AssignmentException extends KontrollException {
    private HttpStatus httpStatus;

    public AssignmentException(HttpStatus status, String message) {
        super(message);
        this.httpStatus = status;
    }

    @Override
    public String getTypeIdentifier() {
        return "assignment-exception";
    }


    @Override
    public HttpStatus getStatus() {
        return httpStatus;
    }
}
