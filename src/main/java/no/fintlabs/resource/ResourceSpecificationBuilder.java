package no.fintlabs.resource;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import no.fintlabs.assignment.Assignment;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class ResourceSpecificationBuilder {

    private final Long userId;
    private final Long roleId;
    private final String resourceType;
    private final List<String> orgUnits;
    private final List<String> orgUnitsInScope;
    private final String searchString;

    public ResourceSpecificationBuilder(
            Long userId,
            Long roleId,
            String resourceType,
            List<String> orgUnits,
            List<String> orgUnitsInScope,
            String searchString
    ) {
        this.userId = userId;
        this.roleId = roleId;
        this.resourceType = resourceType;
        this.orgUnits = orgUnits;
        this.orgUnitsInScope = orgUnitsInScope;
        this.searchString = searchString;
    }

    public Specification<Resource> build() {
        Specification<Resource> spec = (root, query, criteriaBuilder) -> {
            Join<Resource, Assignment> join = assignmentJoin(root);
            return criteriaBuilder.and(
                    criteriaBuilder.isNull(join.get("assignmentRemovedDate")),
                    userId != null ? criteriaBuilder.equal(join.get("userRef"), userId) : criteriaBuilder.equal(join.get("roleRef"), roleId)
            );
        };

        if (!resourceType.equals("ALLTYPES")) {
            spec = spec.and(resourceTypeEquals(resourceType.toLowerCase()));
        }
        if (!isEmptyString(searchString)) {
            spec = spec.and(nameLike(searchString.toLowerCase()));
        }

        return spec;
    }

    private Specification<Resource> resourceTypeEquals(String resourceType) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(criteriaBuilder.lower(root.get("resourceType")), resourceType);
    }

    private Specification<Resource> nameLike(String searchString) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("resourceName")), "%" + searchString + "%");
    }

    private Specification<Resource> belongsToOrgUnit(List<String> orgUnits) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.in(root.get("organisationUnitId")).value(orgUnits);

    }

    private Join<Resource, Assignment> assignmentJoin(Root<Resource> root) {
        return root.join("assignments");
    }

    private boolean isEmptyString(String string) {
        return string == null || string.isEmpty();
    }
}
