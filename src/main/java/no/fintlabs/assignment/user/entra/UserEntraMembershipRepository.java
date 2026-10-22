package no.fintlabs.assignment.user.entra;

import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserEntraMembershipRepository extends JpaRepository<UserEntraMembership, Long> {
    Optional<UserEntraMembership> findByUserEntraIdAndResourceEntraId(UUID userEntraId, UUID resourceEntraId);

    boolean existsByUserEntraIdAndResourceEntraIdAndMembershipStatus(UUID userEntraId, UUID resourceEntraId, MembershipStatus membershipStatus);

    List<UserEntraMembership> findAllByEntraStatus(EntraMembershipSyncStatus entraStatus);

    List<UserEntraMembership> findAllByEntraStatusAndUserEntraId(EntraMembershipSyncStatus entraStatus, UUID userEntraId);

    long countByResourceEntraIdAndMembershipStatus(UUID resourceEntraId, MembershipStatus membershipStatus);
}
