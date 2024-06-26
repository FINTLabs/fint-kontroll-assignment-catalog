package no.fintlabs.assignment.exception;

public class AssignmentMissingAzureGroupIdException extends RuntimeException {

    public AssignmentMissingAzureGroupIdException(Long assignmentId, Long resourceGroupId) {
        super("Tildeling " + assignmentId + " til ressursgruppe " + resourceGroupId
              + " feilet fordi azureAdGroupId mangler");
    }
}
