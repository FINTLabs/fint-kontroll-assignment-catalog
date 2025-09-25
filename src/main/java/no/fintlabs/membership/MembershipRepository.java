package no.fintlabs.membership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, String>, JpaSpecificationExecutor<Membership> {

    List<Membership> findAllByMemberId(Long memberId);

}
