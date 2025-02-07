package no.fintlabs.assignment;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.resource.ResourceRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AssignmentEnforcementService {
    private final ResourceRepository resourceRepository;

    public AssignmentEnforcementService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public void calculateNumberOfResourcesAssigned(Assignment assignment, Long resourceRef) {
        resourceRepository.findById(resourceRef).ifPresent(resource -> {});
    }
}
