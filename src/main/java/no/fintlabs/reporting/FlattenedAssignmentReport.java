package no.fintlabs.reporting;

import java.util.Date;

public record FlattenedAssignmentReport(Long id, String resourceRef, String resourceName, String userOrgUnitRef, String userOrgUnitName, String userType, String applicationResourceLocationOrgUnitId, String applicationResourceLocationOrgUnitName, Date assignmentCreationDate, Date assignmentTerminationDate) {
}
