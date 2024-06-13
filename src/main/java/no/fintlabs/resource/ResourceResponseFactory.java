package no.fintlabs.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ResourceResponseFactory {
    private final AssignmentResourceService assignmentResourceService;
    public ResourceResponseFactory(AssignmentResourceService assignmentResourceService) {
        this.assignmentResourceService = assignmentResourceService;
    }
    public ResponseEntity<Map<String ,Object>> toResponseEntity(
            Long userId,
            Long roleId,
            String resourceType,
            List<String> orgUnits,
            List<String> orgUnitsInScope,
            String searchString,
            int pageNumber,
            int pageSize
    ){
        ResourceSpecificationBuilder builder = new ResourceSpecificationBuilder(userId, roleId, resourceType, orgUnits, orgUnitsInScope,searchString);
        Pageable page = PageRequest.of(pageNumber,
                pageSize,
                Sort.by("resourceName").ascending());

        if (userId != null) {
            return toResponseEntity(assignmentResourceService.getResourcesAssignedToUser(userId, builder.build(), page));
        }
        else {
            return toResponseEntity(assignmentResourceService.getResourcesAssignedToRole(roleId, builder.build(), page));
        }
    }

    public ResponseEntity<Map<String, Object>> toResponseEntity(Page<AssignmentResource> resourcePage) {

        return new ResponseEntity<>(
                Map.of( "resources", resourcePage.getContent(),
                        "currentPage", resourcePage.getNumber(),
                        "totalPages", resourcePage.getTotalPages(),
                        "size", resourcePage.getSize(),
                        "totalItems", resourcePage.getTotalElements()
                ),
                HttpStatus.OK
        );
    }

    public static ResponseEntity<Map<String, Object>> resourceAssignmentUsersToResponseEntity(Page<UserAssignmentResource> resourceAssignmentUsersPagable) {

        return new ResponseEntity<>(
                Map.of( "users", resourceAssignmentUsersPagable.getContent(),
                        "currentPage", resourceAssignmentUsersPagable.getNumber(),
                        "totalPages", resourceAssignmentUsersPagable.getTotalPages(),
                        "size", resourceAssignmentUsersPagable.getSize(),
                        "totalItems", resourceAssignmentUsersPagable.getTotalElements()
                ),
                HttpStatus.OK
        );
    }
}


