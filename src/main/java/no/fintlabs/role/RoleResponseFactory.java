package no.fintlabs.role;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RoleResponseFactory {
    private final AssignmentRoleService assignmentRoleService;
    public RoleResponseFactory(AssignmentRoleService assignmentRoleService) {
        this.assignmentRoleService = assignmentRoleService;
    }
    public ResponseEntity<Map<String ,Object>> toResponseEntity(
            Long resourceId,
            String roleType,
            List<String> orgUnits,
            List<String> orgUnitsInScope,
            String searchString,
            int pageNumber,
            int pageSize
    ){
        RoleSpecificationBuilder builder = new RoleSpecificationBuilder(resourceId, roleType, orgUnits,  orgUnitsInScope, searchString);

        Pageable page = PageRequest.of(pageNumber,
                pageSize,
                Sort.by("roleName").ascending()
                        .and(Sort.by("organisationUnitName")).ascending());

        Page<AssignmentRole> rolesPage = assignmentRoleService.findBySearchCriteria(resourceId, builder.build(), page);
        return toResponseEntity(rolesPage);
    }
    public ResponseEntity<Map<String, Object>> toResponseEntity(Page<AssignmentRole> rolePage) {

        return new ResponseEntity<>(
                Map.of( "roles", rolePage.getContent(),
                        "currentPage", rolePage.getNumber(),
                        "totalPages", rolePage.getTotalPages(),
                        "size", rolePage.getSize(),
                        "totalItems", rolePage.getTotalElements()
                ),
                HttpStatus.OK
        );
    }
}


