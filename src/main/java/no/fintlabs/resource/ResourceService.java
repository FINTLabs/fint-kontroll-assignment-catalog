package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
//import no.vigoiks.resourceserver.security.FintJwtEndRolePrincipal;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ResourceService {
    @Autowired
    private ResourceRepository resourceRepository;

    public Resource save(Resource resource) {
        return resourceRepository.save(resource);
    }
}
