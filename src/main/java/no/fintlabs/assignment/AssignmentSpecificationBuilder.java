package no.fintlabs.assignment;

import org.springframework.data.jpa.domain.Specification;

public class AssignmentSpecificationBuilder {
    public static Specification<Assignment> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("assignmentRemovedDate"));
    }
}
