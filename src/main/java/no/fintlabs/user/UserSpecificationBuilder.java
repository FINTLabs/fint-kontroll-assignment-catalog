package no.fintlabs.user;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.opa.OpaUtils;
import no.fintlabs.opa.model.OrgUnitType;
import no.fintlabs.role.Role;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Slf4j
public class UserSpecificationBuilder {

    private final Long resourceId;
    private final String userType;
    private final List<String> orgUnits;
    private final List<String> orgUnitsInScope;
    private final String searchString;

    public UserSpecificationBuilder(
            Long resourceId,
            String userType,
            List<String> orgUnits,
            List<String> orgUnitsInScope,
            String searchString
    ) {
        this.resourceId = resourceId;
        this.userType = userType;
        this.orgUnits = orgUnits;
        this.orgUnitsInScope = orgUnitsInScope;
        this.searchString = searchString;
    }

    public Specification<User> assignmentSearch() {
        List<String> orgUnitsTofilter = OpaUtils.getOrgUnitsToFilter(orgUnits, orgUnitsInScope);

        Specification<User> spec = (root, query, criteriaBuilder) -> {
            Join<User, Assignment> join = assignmentJoin(root);
            return criteriaBuilder.and(
                    criteriaBuilder.isNull(join.get("assignmentRemovedDate")),
                    criteriaBuilder.equal(join.get("resourceRef"), resourceId)
            );
        };

        if (!orgUnitsTofilter.contains(OrgUnitType.ALLORGUNITS.name())) {
            spec = spec.and(belongsToOrgUnit(orgUnitsTofilter));
        }
        if (!userType.equals("ALLTYPES")) {
            spec = spec.and(userTypeEquals(userType.toLowerCase()));
        }
        if (!OpaUtils.isEmptyString(searchString)) {
            spec = spec.and(nameLike(searchString.toLowerCase()));
        }

        return spec;
    }

    public Specification<FlattenedAssignment> flattenedAssignmentSearch() {
        List<String> orgUnitsTofilter = OpaUtils.getOrgUnitsToFilter(orgUnits, orgUnitsInScope);

        Specification<FlattenedAssignment> spec = (root, query, criteriaBuilder) -> {
            Join<FlattenedAssignment, User> userJoin = root.join("user", JoinType.LEFT);
            Join<FlattenedAssignment, Role> roleJoin = root.join("role", JoinType.LEFT);

            Predicate isNotDeleted = criteriaBuilder.isNull(root.get("assignmentTerminationDate"));
            Predicate resourceRefMatches = criteriaBuilder.equal(root.get("resourceRef"), resourceId);

            if (!userType.equals("ALLTYPES")) {
                criteriaBuilder.and(criteriaBuilder.equal(userJoin.get("userType"), userType.toLowerCase()));
            }

            if (!orgUnitsTofilter.contains(OrgUnitType.ALLORGUNITS.name())) {
                criteriaBuilder.and(criteriaBuilder.in(userJoin.get("organisationUnitId")).value(orgUnits));
            }

            if (!OpaUtils.isEmptyString(searchString)) {
                criteriaBuilder.and(
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("firstName")), "%" + searchString.toLowerCase() + "%"),
                                criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("lastName")), "%" + searchString.toLowerCase() + "%")));
            }

            return criteriaBuilder.and(isNotDeleted, resourceRefMatches);
        };

        return spec;
    }

    private Specification<User> userTypeEquals(String userType) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(criteriaBuilder.lower(root.get("userType")), userType);
    }

    private Specification<User> nameLike(String searchString) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%" + searchString + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), "%" + searchString + "%"));
    }

    private Specification<User> belongsToOrgUnit(List<String> orgUnits) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.in(root.get("organisationUnitId")).value(orgUnits);

    }

    private Join<User, Assignment> assignmentJoin(Root<User> root) {
        return root.join("assignments");
    }

}
