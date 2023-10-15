package no.fintlabs.resource;

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
public class ResourceResponseFactory {
    private final AssignmentResourceService assignmentResourceService;
    public ResourceResponseFactory(AssignmentResourceService assignmentResourceService) {
        this.assignmentResourceService = assignmentResourceService;
    }
    public ResponseEntity<Map<String ,Object>> toResponseEntity(
            Long id,
            int page,
            int size){
        List<AssignmentResource> resources = assignmentResourceService.getResourcesAssignedToUser(id);

        ResponseEntity<Map<String,Object>> entity = toResponseEntity(
                toPage(resources, PageRequest.of(page,size)
                )
        );
        return entity;
    }

//    private Page<Resource> toPage(List<Resource> list, Pageable paging) {
//        int start = (int) paging.getOffset();
//        int end = Math.min((start + paging.getPageSize()), list.size());
//
//        return start > list.size()
//                ? new PageImpl<>(new ArrayList<>(), paging, list.size())
//                : new PageImpl<>(list.subList(start, end), paging, list.size());
//    }
    private Page<AssignmentResource> toPage(List<AssignmentResource> list, Pageable paging) {
        int start = (int) paging.getOffset();
        int end = Math.min((start + paging.getPageSize()), list.size());

        return start > list.size()
                ? new PageImpl<>(new ArrayList<>(), paging, list.size())
                : new PageImpl<>(list.subList(start, end), paging, list.size());
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
}


