package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.opa.OpaService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
public class ResourceController {
    private final ResourceResponseFactory resourceResponseFactory;
    private final OpaService opaService;
    private final AssignmentResourceService assignmentResourceService;

    public ResourceController(ResourceResponseFactory resourceResponseFactory, OpaService opaService, AssignmentResourceService assignmentResourceService) {
        this.resourceResponseFactory = resourceResponseFactory;
        this.opaService = opaService;
        this.assignmentResourceService = assignmentResourceService;
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
        List<String> orgUnitsInScope = opaService.getOrgUnitsInScope("role");
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
        List<String> orgUnitsInScope = opaService.getOrgUnitsInScope("user");
        log.info("Org units returned from scope: {}", orgUnitsInScope);
        log.info("Fetching resources for user with Id: " +userId);
        return resourceResponseFactory.toResponseEntity(userId,null,resourceType, orgUnits, orgUnitsInScope, search, page,size);
    }

    @GetMapping("/v2/role/{roleId}/resources")
    public ResponseEntity<?> getResourcesByRoleId2(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable Long roleId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog" +
                                                                                ".pagesize:20}")
                                                   int size,
                                                   @RequestParam(value = "resourceType", defaultValue = "ALLTYPES")
                                                   String resourceType,
                                                   @RequestParam(value = "orgUnits", required = false)
                                                   List<String> orgUnits,
                                                   @RequestParam(value = "search", required = false) String search
    ) {
        if(roleId == null) {
            return ResponseEntity.badRequest().body("roleId is required");
        }

        List<String> orgUnitsInScope = opaService.getOrgUnitsInScope("role");
        log.info("Org units returned from scope: {}", orgUnitsInScope);

        log.info("Fetching resources for roleId: {}", roleId);
        Page<UserAssignmentResource>
                userAssignmentResources = assignmentResourceService.findUserAssignmentResourcesByRole(roleId, resourceType, orgUnits, orgUnitsInScope, search, page, size);

        return ResourceResponseFactory.resourceAssignmentUsersToResponseEntity(userAssignmentResources);
    }

    @GetMapping("/v2/user/{userId}/resources")
    public ResponseEntity<?> getResourcesByUserId2(@AuthenticationPrincipal Jwt jwt,
                                                                     @PathVariable Long userId,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog" +
                                                                                                  ".pagesize:20}")
                                                                     int size,
                                                                     @RequestParam(value = "resourceType", defaultValue = "ALLTYPES")
                                                                     String resourceType,
                                                                     @RequestParam(value = "orgUnits", required = false)
                                                                     List<String> orgUnits,
                                                                     @RequestParam(value = "search", required = false) String search
    ) {
        if(userId == null) {
            return ResponseEntity.badRequest().body("userId is required");
        }

        List<String> orgUnitsInScope = opaService.getOrgUnitsInScope("user");
        log.info("Org units returned from scope: {}", orgUnitsInScope);

        log.info("Fetching resources for user with Id: " +userId);
        Page<UserAssignmentResource>
                userAssignmentResources = assignmentResourceService.findUserAssignmentResourcesByUser(userId, resourceType, orgUnits, orgUnitsInScope, search, page, size);

        return ResourceResponseFactory.resourceAssignmentUsersToResponseEntity(userAssignmentResources);
    }
}
