package no.fintlabs.assignment;

public class AssignmentAlreadyExistsException extends RuntimeException{
    public AssignmentAlreadyExistsException(String assigneeId, String resourceId) {
        super("Ressurs " + resourceId + " er allerede tildelt bruker/rolle " + assigneeId);
    }
}
