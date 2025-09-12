package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ResourceService {
    private ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }
    public Resource save(Resource resource) {
        return resourceRepository.save(resource);
    }
    public Optional<Resource> findRoleById(Long id) {
        return resourceRepository.findById(id);
    }

    public List<Resource> findAll() {
        return resourceRepository.findAll();
    }
}
