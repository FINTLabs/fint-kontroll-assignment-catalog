package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.opa.OpaService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/v2/resource/{id}/users")
    public ResponseEntity<Map<String, Object>> getAssignedUsersForResource(@PathVariable Long id,
                                                                             @RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog.pagesize:20}") int size,
                                                                             @RequestParam(defaultValue = "ALLTYPES") String userType,
                                                                             @RequestParam(required = false) List<String> orgUnits,
                                                                             @RequestParam(required = false) String search,
                                                                             @RequestParam(name = "userIds", required = false) List<Long> userIds,
                                                                             @RequestParam(name = "userfilter", required = false) List<Long> legacyUserIds
    ) {
        long start = System.currentTimeMillis();
        List<String> orgUnitsInScope = opaService.getOrgUnitsInScope("user");
        List<Long> effectiveUserIds = userIds != null ? userIds : legacyUserIds;

        Page<ResourceAssignmentUser> resourceAssignments
                = assignmentUserService.findResourceAssignmentUsersForResourceId(
                id,
                userType,
                orgUnits,
                orgUnitsInScope,
                effectiveUserIds,
                search,
                page,
                size
        );
        log.info("Fetched {} assigned users for resource {} in {} ms",
                resourceAssignments.getNumberOfElements(), id, System.currentTimeMillis() - start);
        return UserResponseFactory.resourceAssignmentUsersToResponseEntity(resourceAssignments);
    }
}
