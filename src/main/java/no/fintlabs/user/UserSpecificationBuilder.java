package no.fintlabs.user;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.opa.OpaUtils;
import no.fintlabs.opa.model.OrgUnitType;
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
    ){
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

    public Specification<User> flattenedAssignmentSearch() {
        List<String> orgUnitsTofilter = OpaUtils.getOrgUnitsToFilter(orgUnits, orgUnitsInScope);

        Specification<User> spec = (root, query, criteriaBuilder) -> {
            root.join("flattenedAssignments")
                    .join("role", JoinType.LEFT);

            Predicate
                    isNotDeleted = criteriaBuilder.isFalse(root.get("flattenedAssignments").get("identityProviderGroupMembershipDeletionConfirmed"));
            Predicate userRefMatches = criteriaBuilder.equal(root.get("flattenedAssignments").get("userRef"), root.get("id"));
            Predicate resourceRefMatches = criteriaBuilder.equal(root.get("flattenedAssignments").get("resourceRef"), resourceId);

//            query.distinct(true);

            return criteriaBuilder.and(isNotDeleted, userRefMatches, resourceRefMatches);
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

    private  Specification<User> userTypeEquals(String userType) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(criteriaBuilder.lower(root.get("userType")), userType);
    }

    private Specification<User> nameLike(String searchString) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%" + searchString + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), "%" + searchString + "%"));
    }

    private Specification<User> belongsToOrgUnit(List<String> orgUnits) {
        return (root, query, criteriaBuilder)-> criteriaBuilder.in(root.get("organisationUnitId")).value(orgUnits);

    }

    private Join<User, Assignment> assignmentJoin(Root<User> root){
        return root.join("assignments");
    }

}
