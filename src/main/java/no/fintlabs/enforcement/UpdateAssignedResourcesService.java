package no.fintlabs.enforcement;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.resource.ResourceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateAssignedResourcesService {
    private static final int RECONCILIATION_BATCH_SIZE = 500;

    private final ResourceRepository resourceRepository;
    private final LicenseEnforcementService licenseEnforcementService;

    public int updateAssignedResources() {
        log.info("Start updating assignedResources initiated by api call");
        long lastProcessedId = 0L;
        int updatedResources = 0;
        List<Long> resourceIds;

        do {
            resourceIds = resourceRepository.findIdsByIdGreaterThanOrderByIdAsc(
                    lastProcessedId,
                    PageRequest.of(0, RECONCILIATION_BATCH_SIZE));

            resourceIds.forEach(licenseEnforcementService::recalculateAssignedResourcesForResource);
            updatedResources += resourceIds.size();

            if (!resourceIds.isEmpty()) {
                lastProcessedId = resourceIds.getLast();
            }
        } while (resourceIds.size() == RECONCILIATION_BATCH_SIZE);

        log.info("Finished updating assignedResources for {} resources", updatedResources);
        return updatedResources;
    }
}
