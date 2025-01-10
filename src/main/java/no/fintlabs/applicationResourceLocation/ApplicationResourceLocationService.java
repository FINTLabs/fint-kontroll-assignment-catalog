package no.fintlabs.applicationResourceLocation;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class ApplicationResourceLocationService {

    private final ApplicationResourceLocationRepository applicationResourceLocationRepository;

    public ApplicationResourceLocationService(ApplicationResourceLocationRepository applicationResourceLocationRepository) {
        this.applicationResourceLocationRepository = applicationResourceLocationRepository;
    }

    public void save(ApplicationResourceLocation resourceLocation) {
        log.info("Saving applicationResourceLocation with id: {} - for resource: {}", resourceLocation.id, resourceLocation.resourceId);
        applicationResourceLocationRepository.save(resourceLocation);
    }
}
