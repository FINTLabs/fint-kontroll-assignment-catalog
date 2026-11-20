package no.fintlabs.assignment;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AssignmentResponseFactory {

    private final AssignmentRepository assignmentRepository;

    public ResponseEntity<Map<String, Object>> toResponseEntity(int pageNumber, int pageSize) {
        Specification<Assignment> spec = AssignmentSpecificationBuilder.notDeleted();
        List<Assignment> allAssignments = assignmentRepository.findAll(spec);

        return toResponseEntity(toPage(allAssignments.stream()
                                               .map(Assignment::toSimpleAssignment)
                                               .toList(), PageRequest.of(pageNumber, pageSize))
        );
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
