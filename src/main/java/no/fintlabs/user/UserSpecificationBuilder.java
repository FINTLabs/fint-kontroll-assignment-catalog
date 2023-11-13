package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.opa.model.OrgUnitType;
import no.fintlabs.utils.Utils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.List;

@Slf4j
public class UserSpecificationBuilder {
    private final Long resourceId;
    private final String userType;
    private final List<String> orgUnits;
    private final List<String> orgUnitsInScope;
    private final String searchString;
    private final Utils utils;
    public UserSpecificationBuilder(
            Long resourceId,
            String userType,
            List<String> orgUnits,
            List<String> orgUnitsInScope,
            String searchString,
            Utils utils
    ){
        this.resourceId = resourceId;
        this.userType = userType;
        this.orgUnits = orgUnits;
        this.orgUnitsInScope = orgUnitsInScope;
        this.searchString = searchString;
        this.utils = utils;
    }
    public Specification<User> build() {
        Specification<User> spec = resourceEquals(resourceId);
        List<String> orgUnitsTofilter = utils.getOrgUnitsToFilter(orgUnits, orgUnitsInScope);

        if (!orgUnitsTofilter.contains(OrgUnitType.ALLORGUNITS.name())) {
            spec = spec.and(belongsToOrgUnit(orgUnitsTofilter));
        }
        if (!userType.equals("ALLTYPES")) {
            spec = spec.and(userTypeEquals(userType.toLowerCase()));
        }
        if (!utils.isEmptyString(searchString)) {
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
