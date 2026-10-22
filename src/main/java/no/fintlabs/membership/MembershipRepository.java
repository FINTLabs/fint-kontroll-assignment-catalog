package no.fintlabs.membership;

import no.fintlabs.entra.membership.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, String>, JpaSpecificationExecutor<Membership> {

    List<Membership> findAllByMemberId(Long memberId);

    @Query("""
            SELECT COUNT(DISTINCT membership.identityProviderUserObjectId)
            FROM Membership membership
            WHERE membership.roleId = :roleId
            AND UPPER(membership.memberStatus) = 'ACTIVE'
            AND membership.identityProviderUserObjectId IS NOT NULL
            AND NOT EXISTS (
                SELECT 1
                FROM UserEntraMembership userEntraMembership
                WHERE userEntraMembership.userEntraId = membership.identityProviderUserObjectId
                AND userEntraMembership.resourceEntraId = :resourceEntraId
                AND userEntraMembership.membershipStatus = :membershipStatus
            )
            """)
    long countActiveRoleMembersWithoutActiveEntraMembership(
            @Param("roleId") Long roleId,
            @Param("resourceEntraId") UUID resourceEntraId,
            @Param("membershipStatus") MembershipStatus membershipStatus);

}
