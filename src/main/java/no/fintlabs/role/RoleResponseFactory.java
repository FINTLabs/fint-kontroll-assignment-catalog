package no.fintlabs.role;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RoleResponseFactory {
    private final AssignmentRoleService assignmentRoleService;
    public RoleResponseFactory(AssignmentRoleService assignmentRoleService) {
        this.assignmentRoleService = assignmentRoleService;
    }
    public ResponseEntity<Map<String ,Object>> toResponseEntity(
            Long id,
            int page,
            int size){
        List<AssignmentRole> roles = assignmentRoleService.getRolesAssignedToResource(id);

        ResponseEntity<Map<String,Object>> entity = toResponseEntity(
                toPage(roles, PageRequest.of(page,size)
                )
        );
        return entity;
    }
    private Page<AssignmentRole> toPage(List<AssignmentRole> list, Pageable paging) {
        int start = (int) paging.getOffset();
        int end = Math.min((start + paging.getPageSize()), list.size());

        return start > list.size()
                ? new PageImpl<>(new ArrayList<>(), paging, list.size())
                : new PageImpl<>(list.subList(start, end), paging, list.size());
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


