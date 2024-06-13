package no.fintlabs.resource;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.role.Role;
import no.fintlabs.user.User;
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
        //        List<String> orgUnitsTofilter = OpaUtils.getOrgUnitsToFilter(orgUnits, orgUnitsInScope);
        //        if (!orgUnitsTofilter.contains(OrgUnitType.ALLORGUNITS.name())) {
        //            spec = spec.and(belongsToOrgUnit(orgUnitsTofilter));
        //        }
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

    public Specification<FlattenedAssignment> flattenedAssignmentSearchByUser() {
        Specification<FlattenedAssignment> spec = (root, query, criteriaBuilder) -> {
            Join<FlattenedAssignment, User> userJoin = root.join("user", JoinType.LEFT);
            Join<FlattenedAssignment, Role> roleJoin = root.join("role", JoinType.LEFT);
            Join<FlattenedAssignment, Resource> resourceJoin = root.join("resource", JoinType.LEFT);

            Predicate isNotDeleted = criteriaBuilder.isNull(root.get("assignmentTerminationDate"));
            Predicate refMatches = criteriaBuilder.equal(root.get("userRef"), userId);

            if (!resourceType.equals("ALLTYPES")) {
                criteriaBuilder.and(criteriaBuilder.equal(resourceJoin.get("resourceType"), resourceType.toLowerCase()));
            }

            if (!isEmptyString(searchString)) {
                criteriaBuilder.and(criteriaBuilder.like(criteriaBuilder.lower(resourceJoin.get("resourceName")), "%" + searchString.toLowerCase() + "%"));
            }

            return criteriaBuilder.and(isNotDeleted, refMatches);
        };

        return spec;
    }

    public Specification<FlattenedAssignment> flattenedAssignmentSearchByRole() {
        Specification<FlattenedAssignment> spec = (root, query, criteriaBuilder) -> {
            Join<FlattenedAssignment, User> userJoin = root.join("user", JoinType.LEFT);
            Join<FlattenedAssignment, Role> roleJoin = root.join("role", JoinType.LEFT);
            Join<FlattenedAssignment, Resource> resourceJoin = root.join("resource", JoinType.LEFT);

            Predicate isNotDeleted = criteriaBuilder.isNull(root.get("assignmentTerminationDate"));
            Predicate refMatches = criteriaBuilder.equal(root.get("assignmentViaRoleRef"), roleId);

            if (!resourceType.equals("ALLTYPES")) {
                criteriaBuilder.and(criteriaBuilder.equal(resourceJoin.get("resourceType"), resourceType.toLowerCase()));
            }

            if (!isEmptyString(searchString)) {
                criteriaBuilder.and(criteriaBuilder.like(criteriaBuilder.lower(resourceJoin.get("resourceName")), "%" + searchString.toLowerCase() + "%"));
            }

            return criteriaBuilder.and(isNotDeleted, refMatches);
        };

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
