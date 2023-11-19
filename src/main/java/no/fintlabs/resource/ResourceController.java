package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.opa.OpaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
public class ResourceController {
    private final ResourceResponseFactory resourceResponseFactory;
    private final OpaService opaService;

    public ResourceController(ResourceResponseFactory resourceResponseFactory, OpaService opaService) {
        this.resourceResponseFactory = resourceResponseFactory;
        this.opaService = opaService;
    }
    @GetMapping("role/{roleId}/resources")
    public ResponseEntity<Map<String , Object>> getResourcesByRoleId(@AuthenticationPrincipal Jwt jwt,
                                                                     @PathVariable Long roleId,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog.pagesize:20}") int size,
                                                                     @RequestParam(value = "resourceType", defaultValue = "ALLTYPES") String resourceType,
                                                                     @RequestParam(value= "orgUnits", required = false) List<String> orgUnits,
                                                                     @RequestParam(value = "search", required = false) String search
    ){
        List<String> orgUnitsInScope = opaService.getOrgUnitsInScope();
        log.info("Org units returned from scope: {}", orgUnitsInScope);
        log.info("Fetching resources for role with Id: " + roleId);
        return resourceResponseFactory.toResponseEntity(null,roleId,resourceType, orgUnits, orgUnitsInScope, search, page,size);
    }
    @GetMapping("user/{userId}/resources")
    public ResponseEntity<Map<String , Object>> getResourcesByUserId(@AuthenticationPrincipal Jwt jwt,
                                                                         @PathVariable Long userId,
                                                                         @RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog.pagesize:20}") int size,
                                                                         @RequestParam(value = "resourceType", defaultValue = "ALLTYPES") String resourceType,
                                                                         @RequestParam(value= "orgUnits", required = false) List<String> orgUnits,
                                                                         @RequestParam(value = "search", required = false) String search
    ){
        List<String> orgUnitsInScope = opaService.getOrgUnitsInScope();
        log.info("Org units returned from scope: {}", orgUnitsInScope);
        log.info("Fetching resources for user with Id: " +userId);
        return resourceResponseFactory.toResponseEntity(userId,null,resourceType, orgUnits, orgUnitsInScope, search, page,size);
    }
}
