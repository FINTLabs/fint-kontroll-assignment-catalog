package no.fintlabs.role;

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
public class RoleController {
    private final  RoleResponseFactory roleResponseFactory;
    private final OpaService opaService;
    public RoleController(RoleResponseFactory roleResponseFactory, OpaService opaService) {
        this.roleResponseFactory = roleResponseFactory;
        this.opaService = opaService;
    }

    @GetMapping("resource/{id}/roles")
    public ResponseEntity<Map<String , Object>> getRolesByResourceId(@AuthenticationPrincipal Jwt jwt,
                                                                     @PathVariable Long id,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog.pagesize:20}") int size,
                                                                     @RequestParam(value = "roleType", defaultValue = "ALLTYPES") String roleType,
                                                                     @RequestParam(value= "orgUnits", required = false) List<String> orgUnits,
                                                                     @RequestParam(value = "search", required = false) String search
    ){
        List<String> orgUnitsInScope = opaService.getOrgUnitsInScope("role");
        log.info("Org units returned from scope: {}", orgUnitsInScope);
        log.info("Fetching roles for resource with Id: " +id);
        return roleResponseFactory.toResponseEntity(id, roleType, orgUnits, orgUnitsInScope,search, page,size);
    }
}
