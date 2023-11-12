package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
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

    public ResourceController( ResourceResponseFactory resourceResponseFactory) {
        this.resourceResponseFactory = resourceResponseFactory;
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
        log.info("Fetching resources for role with Id: " + roleId);
        return resourceResponseFactory.toResponseEntity(null,roleId,resourceType, orgUnits, search, page,size);
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
        log.info("Fetching resources for user with Id: " +userId);
        return resourceResponseFactory.toResponseEntity(userId,null,resourceType, orgUnits, search, page,size);
    }
}
