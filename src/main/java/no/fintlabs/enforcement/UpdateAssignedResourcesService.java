package no.fintlabs.enforcement;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateAssignedResourcesService {
    private final ResourceRepository resourceRepository;
    private final LicenseEnforcementService licenseEnforcementService;

    @Transactional
    public int updateAssignedResources() {
        log.info("Start updating assignedResources initiated by api call");
        List<Resource> resources = resourceRepository.findAll();
        log.info("Resources to update: {}", resources.size());
        resources.forEach(resource -> licenseEnforcementService.recalculateAssignedResourcesForResource(resource.getId()));
        resourceRepository.flush();
        return resources.size();
    }
}
