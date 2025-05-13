package no.fintlabs.assignment;

import no.fintlabs.membership.Membership;
import org.springframework.data.jpa.domain.Specification;

public class MembershipSpecificationBuilder {

    public static Specification<Membership> hasRoleId(Long roleId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("roleId"), roleId);
    }
    public static Specification<Membership> memberShipIsActive() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(criteriaBuilder.lower(root.get("memberStatus")), "active");
    }
    public static Specification<Membership> hasIdentityProviderUserObjectId() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get("identityProviderUserObjectId"));
    }
    public static Specification<Membership> hasIdentityProviderGroupId() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get("identityProviderGroupId"));
    }
}
