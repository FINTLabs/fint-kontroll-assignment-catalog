package no.fintlabs.role;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.exception.AssignmentException;
import no.fintlabs.opa.OpaService;
import no.fintlabs.user.UserResponseFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
public class RoleController {

    private final OpaService opaService;
    private final AssignmentRoleService assignmentRoleService;

    public RoleController(AssignmentRoleService assignmentRoleService, OpaService opaService) {
        this.assignmentRoleService = assignmentRoleService;
        this.opaService = opaService;
    }

    @GetMapping("resource/{id}/roles")
    public ResponseEntity<?> getRolesByResourceId(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable Long id,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog.pagesize:20}") int size,
                                                  @RequestParam(value = "roleType", defaultValue = "ALLTYPES") String roleType,
                                                  @RequestParam(value = "orgUnits", required = false) List<String> orgUnits,
                                                  @RequestParam(value = "search", required = false) String search,
                                                  @RequestParam(value = "rolefilter", required = false) List<Long> roleIds
    ) {
        if (id == null) {
            throw new AssignmentException(HttpStatus.BAD_REQUEST, "Role id is required");
        }

        List<String> orgUnitsInScope = opaService.getOrgUnitsInScope("role");
        log.info("Org units returned from scope: {}", orgUnitsInScope);
        log.info("Fetching roles for resource with Id: " + id);

        RoleSpecificationBuilder builder = new RoleSpecificationBuilder(id, roleType, orgUnits, orgUnitsInScope, roleIds, search);

        Pageable resourceRolesPagination = PageRequest.of(page, size,
                                                          Sort.by("roleName").ascending()
                                                                  .and(Sort.by("organisationUnitName")).ascending());

        Page<AssignmentRole> resourceRoles = assignmentRoleService.findBySearchCriteria(id, builder.build(), resourceRolesPagination);

        return UserResponseFactory.assignmentRoleToResponseEntity(resourceRoles);
    }
}
