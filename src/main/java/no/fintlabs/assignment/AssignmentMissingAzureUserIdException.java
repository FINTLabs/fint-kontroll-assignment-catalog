package no.fintlabs.assignment;

public class AssignmentMissingAzureUserIdException extends RuntimeException{
    public AssignmentMissingAzureUserIdException(Long assignmentId, Long userId) {
        super("Tildeling " + assignmentId + " til bruker " + userId
                + " feilet fordi azureAdUserId mangler");
    }
}
