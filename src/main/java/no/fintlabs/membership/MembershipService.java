package no.fintlabs.membership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.membership.Membership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MembershipService {
    @Autowired
    private MembershipRepository membershipRepository;
    public Membership save(Membership membership) {
        return membershipRepository.save(membership);
    }

    public List<Membership> getMembersAssignedToRole (Specification specification) {
        return membershipRepository.findAll(specification);
    }
    public List<Membership> getRolesAssignedToMember (Specification specification) {
        return membershipRepository.findAll(specification);
    }
}
