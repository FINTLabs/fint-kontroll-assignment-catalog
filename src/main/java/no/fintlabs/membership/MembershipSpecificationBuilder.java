package no.fintlabs.membership;

import no.fintlabs.resource.Resource;
import org.springframework.data.jpa.domain.Specification;

public class MembershipSpecificationBuilder {
    private final Long roleId;
    public MembershipSpecificationBuilder(Long roleId) {
        this.roleId = roleId;
    }
    public Specification<Membership> roleEquals(Long roleId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("roleId"), roleId);
    }
}
