package no.fintlabs.assignment;

public class AssignmentAlreadyExistsException extends RuntimeException{
    public AssignmentAlreadyExistsException(String userId, String resourceId) {
        super("Ressurs " + resourceId + " er allerede tildelt bruker " + userId);
    }
}
