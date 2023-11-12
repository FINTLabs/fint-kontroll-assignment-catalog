package no.fintlabs.resource;

import no.fintlabs.assignment.Assignment;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.List;

public class ResourceSpecificationBuilder {
    private final Long userId;
    private final Long roleId;
    private final String resourceType;
    private final List<String> orgUnits;
    private final String searchString;

    public ResourceSpecificationBuilder(Long userId, Long roleId, String resourceType, List<String> orgUnits, String searchString){
        this.userId = userId;
        this.roleId = roleId;
        this.resourceType = resourceType;
        this.orgUnits = orgUnits;
        this.searchString = searchString;
    }
    public Specification<Resource> build() {
        Specification<Resource> spec = userId != null ? userEquals(userId) : roleEquals(roleId);

        if (!resourceType.equals("ALLTYPES")) {
            spec = spec.and(resourceTypeEquals(resourceType.toLowerCase()));
        }
        if (!isEmptyString(searchString)) {
            spec = spec.and(nameLike(searchString.toLowerCase()));
        }
        if (orgUnits!=null && !orgUnits.isEmpty()) {
            spec = spec.and(belongsToOrgUnit(orgUnits));
        }
        return spec;
    }
    private Specification<Resource> userEquals(Long userId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(assignmentJoin(root).get("userRef"), userId);
    }
    private Specification<Resource> roleEquals(Long roleId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(assignmentJoin(root).get("roleRef"), roleId);
    }
    private  Specification<Resource> resourceTypeEquals(String resourceType) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(criteriaBuilder.lower(root.get("resourceType")), resourceType);
    }
    private Specification<Resource> nameLike(String searchString) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%" + searchString + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), "%" + searchString + "%"));
    }
    private Specification<Resource> belongsToOrgUnit(List<String> orgUnits) {
        return (root, query, criteriaBuilder)-> criteriaBuilder.in(root.get("organisationUnitId")).value(orgUnits);

    }
    private Join<Resource, Assignment> assignmentJoin(Root<Resource> root){
        return root.join("assignments");
    }
    private boolean isEmptyString(String string) {
        return string == null || string.length() == 0;
    }
}
