package no.fintlabs.assignment;

import no.fintlabs.membership.Membership;
import org.springframework.data.jpa.domain.Specification;

public class MembershipSpecificationBuilder {

    public static Specification<Membership> hasRoleId(Long roleId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("roleId"), roleId);
    }
}
