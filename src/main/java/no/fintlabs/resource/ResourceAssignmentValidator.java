package no.fintlabs.resource;

import lombok.RequiredArgsConstructor;
import no.fintlabs.assignment.exception.AssignmentException;
import no.fintlabs.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceAssignmentValidator {
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final ResourceRepository resourceRepository;

    public Resource getActiveResourceOrThrow(Long resourceRef) {
        Resource resource = resourceRepository.findById(resourceRef)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceRef));

        if (!ACTIVE_STATUS.equalsIgnoreCase(resource.getStatus())) {
            throw new AssignmentException(HttpStatus.UNPROCESSABLE_ENTITY, "Resource " + resourceRef + " is not active");
        }

        if (resource.getIdentityProviderGroupObjectId() == null) {
            throw new AssignmentException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Resource " + resourceRef + " does not have entra group id set"
            );
        }

        return resource;
    }
}
