package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.exception.AssignmentException;
import no.fintlabs.opa.OpaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
public class UserController {

    private final AssignmentUserService assignmentUserService;
    private final OpaService opaService;

    public UserController(AssignmentUserService assignmentUserService, OpaService opaService) {
        this.assignmentUserService = assignmentUserService;
        this.opaService = opaService;
    }

    @GetMapping("resource/{id}/users")
    public ResponseEntity<Map<String, Object>> getUsersByResourceId(@AuthenticationPrincipal Jwt jwt,
                                                                    @PathVariable Long id,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog" +
                                                                            ".pagesize:20}")
                                                                    int size,
                                                                    @RequestParam(value = "userType", defaultValue = "ALLTYPES")
                                                                    String userType,
                                                                    @RequestParam(value = "orgUnits", required = false)
                                                                    List<String> orgUnits,
                                                                    @RequestParam(value = "search", required = false) String search
    ) {
        List<String> orgUnitsInScope = opaService.getOrgUnitsInScope("user");
        log.info("Org units returned from scope: {}", orgUnitsInScope);
        log.info("Fetching users for resource with Id: " + id);

        Specification<User> spec = new UserSpecificationBuilder(id, userType, orgUnits, orgUnitsInScope, search)
                .assignmentSearch();

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("firstName").ascending()
                        .and(Sort.by("lastName")).ascending());

        Page<AssignmentUser> usersPage = assignmentUserService.findBySearchCriteria(id, spec, pageable);

        return UserResponseFactory.assignmentUsersToResponseEntity(usersPage);
    }

    @GetMapping("/v2/resource/{id}/users")
    public ResponseEntity<?> getUsersByResourceId2(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable("id") Long id,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog" +
                                                           ".pagesize:20}")
                                                   int size,
                                                   @RequestParam(value = "userType", defaultValue = "ALLTYPES")
                                                   String userType,
                                                   @RequestParam(value = "orgUnits", required = false)
                                                   List<String> orgUnits,
                                                   @RequestParam(value = "search", required = false) String search,
                                                   @RequestParam(value = "userfilter", required = false) List<Long> userIds
    ) {
        if (id == null) {
            throw new AssignmentException(
                    HttpStatus.BAD_REQUEST,
                    "Resource id is required"
            );
        }
        List<String> orgUnitsInScope = opaService.getOrgUnitsInScope("user");
        log.info("Org units returned from scope: {}", orgUnitsInScope);


        Page<ResourceAssignmentUser> resourceAssignments
                = assignmentUserService.findResourceAssignmentUsersForResourceId(
                id,
                userType,
                orgUnits,
                orgUnitsInScope,
                userIds,
                search,
                page,
                size
        );
        log.info("Resource assignment users returned from assignmentUserService");

        if (resourceAssignments == null) {
            throw new AssignmentException(
                    HttpStatus.NOT_FOUND,
                    "Fetching users assigned to resource " + id + " resources returned no users"
            );
        }
        return UserResponseFactory.resourceAssignmentUsersToResponseEntity(resourceAssignments);


    }
}
