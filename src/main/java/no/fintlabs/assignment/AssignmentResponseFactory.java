package no.fintlabs.assignment;

import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class AssignmentResponseFactory {
    //private final FintFilterService fintFilterService;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentService assignmentService;

    public AssignmentResponseFactory( AssignmentRepository assignmentRepository, AssignmentService assignmentService) //FintFilterService fintFilterService,
    {
        //this.fintFilterService = fintFilterService;
        this.assignmentRepository = assignmentRepository;
        this.assignmentService = assignmentService;
    }
    public ResponseEntity<Map<String, Object>> toResponseEntity(FintJwtEndUserPrincipal principal,
                                                                String search,
                                                                List<String> orgUnits,
                                                                int page,
                                                                int size
    ){
        List<SimpleAssignment> simpleAssignments = assignmentService.getSimpleAssignments();//,search,orgUnits
        ResponseEntity<Map<String,Object>> entity = toResponseEntity(toPage(simpleAssignments,PageRequest.of(page,size)));

        return entity;
    }
    public ResponseEntity<Map<String, Object>> toResponseEntity(
            FintJwtEndUserPrincipal principal,
            //String filter,
            int pageNumber,
            int pageSize,
            String userType
    ) {
        Pageable page = PageRequest.of(pageNumber,
                pageSize,
                Sort.by("resourceName").ascending()
                        .and(Sort.by("userDisplayname")).ascending());

        //Page<SimpleAssignment> assignmentPage = assignmentRepository.findAll(page);
        //return toResponseEntity(assignmentPage);

        Stream<Assignment> assignmentStream = assignmentRepository.findAll().stream();
        ResponseEntity<Map<String, Object>> entity = toResponseEntity(
//                toPage(
//                        StringUtils.hasText(filter)
//                                ? fintFilterService
//                                .from(assignmentStream, filter)
//                                .map(Assignment::toSimpleAssignment).toList()
//                                : assignmentStream.map(Assignment::toSimpleAssignment).toList(),
//                        PageRequest.of(page, size)
//                )
                toPage(
                        assignmentStream.map(Assignment::toSimpleAssignment).toList(),
                        PageRequest.of(pageNumber, pageSize))
        );
        return entity;
    }

    private Page<SimpleAssignment> toPage(List<SimpleAssignment> list, Pageable paging) {
        int start = (int) paging.getOffset();
        int end = Math.min((start + paging.getPageSize()), list.size());

        return start > list.size()
                ? new PageImpl<>(new ArrayList<>(), paging, list.size())
                : new PageImpl<>(list.subList(start, end), paging, list.size());
    }

    public ResponseEntity<Map<String, Object>> toResponseEntity(Page<SimpleAssignment> rolePage) {

        return new ResponseEntity<>(
                Map.of(
                        "assignments", rolePage.getContent(),
                        "currentPage", rolePage.getNumber(),
                        "totalPages", rolePage.getTotalPages(),
                        "size", rolePage.getSize(),
                        "totalItems", rolePage.getTotalElements()
                ),
                HttpStatus.OK
        );
    }
}
