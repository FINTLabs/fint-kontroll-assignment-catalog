package no.fintlabs.role;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.resource.ResourceService;
import no.fintlabs.resource.ResourceResponseFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
public class RoleController {
    private final ResourceResponseFactory resourceResponseFactory;
    private final ResourceService resourceService;

    public RoleController(ResourceResponseFactory resourceResponseFactory, ResourceService resourceService) {
        this.resourceResponseFactory = resourceResponseFactory;
        this.resourceService = resourceService;
    }

    @GetMapping("role/{id}/resources")
    public ResponseEntity<Map<String , Object>> getResourcesByRoleId(@AuthenticationPrincipal Jwt jwt,
                                                                         @PathVariable Long id,
                                                                         @RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog.pagesize:20}") int size){
        log.info("Fetching resources for role with Id: " +id);
        return resourceResponseFactory.toResponseEntity(id,page,size);
    }
}
