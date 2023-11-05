package no.fintlabs.user;

import no.fintlabs.assignment.Assignment;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.List;

public class UserSpecificationBuilder {

    private final Long resourceId;
    private final String userType;
    private final List<String> orgUnits;
    private final String searchString;

    public UserSpecificationBuilder(Long resourceId, String userType, List<String> orgUnits, String searchString){
        this.resourceId = resourceId;
        this.userType = userType;
        this.orgUnits = orgUnits;
        this.searchString = searchString;
    }
    public Specification<User> build() {
        Specification<User> spec = resourceEquals(resourceId);

        if (!userType.equals("ALLTYPES")) {
            spec = spec.and(userTypeEquals(userType.toLowerCase()));
        }
        if (!isEmptyString(searchString)) {
            spec = spec.and(nameLike(searchString.toLowerCase()));
        }
//        if (!orgUnits.contains("ALLORGUNITS")) {
//            spec = spec.and(belongToOrgUnit(orgUnits));
//        }
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
//    private Specification<User> belongToOrgUnit(List<String> orgUnits) {
//        return (root, query, criteriaBuilder)-> criteriaBuilder.in(root.get(), orgUnits);
//
//    }

    private Join<User, Assignment> resourceJoin(Root<User> root){
        return root.join("assignments");

    }
    private boolean isEmptyString(String string) {
        return string == null || string.length() == 0;
    }
}
