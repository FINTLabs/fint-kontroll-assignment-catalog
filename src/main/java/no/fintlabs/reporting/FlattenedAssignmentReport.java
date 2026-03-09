package no.fintlabs.reporting;

import java.util.Date;

public record FlattenedAssignmentReport(Long id, String resourceRef, String resourceName, String resourceId,
                                        String identityProviderGroupName, String userOrgUnitRef, String userOrgUnitName, String userType, String licenseConsumerOrgUnitRef,
                                        String licenseConsumerOrgUnitName, Date assignmentStartDate, Date assignmentEndDate) {
}
