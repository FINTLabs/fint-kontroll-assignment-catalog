package no.fintlabs.role;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.opa.OpaUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class RoleSpecificationBuilder {

    private final Long resourceId;
    private final String roleType;
    private final List<String> orgUnits;
    private final List<String> orgUnitsInScope;
    private final String searchString;

    private List<Long> roleIds;

    public RoleSpecificationBuilder(Long resourceId, String roleType, List<String> orgUnits, List<String> orgUnitsInScope, List<Long> roleIds, String searchString) {
        this.resourceId = resourceId;
        this.roleType = roleType;
        this.orgUnits = orgUnits;
        this.orgUnitsInScope = orgUnitsInScope;
        this.searchString = searchString;
        this.roleIds = roleIds;
    }

    public Specification<Role> build() {
        List<String> orgUnitsTofilter = OpaUtils.getOrgUnitsToFilter(orgUnits, orgUnitsInScope);

        Specification<Role> spec = (root, query, criteriaBuilder) -> {
            Join<Role, Assignment> join = assignmentsJoin(root);
            return criteriaBuilder.and(
                    criteriaBuilder.isNull(join.get("assignmentRemovedDate")),
                    criteriaBuilder.equal(join.get("resourceRef"), resourceId)
            );
        };

        if (!orgUnitsTofilter.contains("ALLORGUNITS")) {
            spec = spec.and(belongsToOrgUnit(orgUnitsTofilter));
        }

        if (!roleType.equals("ALLTYPES")) {
            spec = spec.and(roleTypeEquals(roleType.toLowerCase()));
        }

        if (!isEmptyString(searchString)) {
            spec = spec.and(nameLike(searchString.toLowerCase()));
        }

        if(roleIds != null && !roleIds.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.in(root.get("id")).value(roleIds));
        }
        return spec;
    }

    private Specification<Role> resourceEquals(Long resourceId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(assignmentsJoin(root).get("resourceRef"), resourceId);
    }

    private Specification<Role> roleTypeEquals(String roleType) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(criteriaBuilder.lower(root.get("roleType")), roleType);
    }

    private Specification<Role> nameLike(String searchString) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("roleName")), "%" + searchString + "%");
    }

    private Specification<Role> belongsToOrgUnit(List<String> orgUnits) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.in(root.get("organisationUnitId")).value(orgUnits);

    }

    private Join<Role, Assignment> assignmentsJoin(Root<Role> root) {
        return root.join("assignments");

    }

    private boolean isEmptyString(String string) {
        return string == null || string.isEmpty();
    }
}
