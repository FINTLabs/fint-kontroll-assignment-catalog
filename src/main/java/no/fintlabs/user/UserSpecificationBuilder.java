package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.opa.OpaUtils;
import no.fintlabs.opa.model.OrgUnitType;
import no.fintlabs.opa.OpaUtils;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
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
    public Specification<User> build() {
        Specification<User> spec = resourceEquals(resourceId);

        List<String> orgUnitsTofilter = OpaUtils.getOrgUnitsToFilter(orgUnits, orgUnitsInScope);

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

    private Specification<User> resourceEquals(Long resourceId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(resourceJoin(root).get("resourceRef"), resourceId);
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

    private Join<User, Assignment> resourceJoin(Root<User> root){
        return root.join("assignments");
    }

}
