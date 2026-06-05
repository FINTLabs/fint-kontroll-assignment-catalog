package no.fintlabs.assignment.entra;

import no.fintlabs.entra.EntraStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserEntraMembershipRepository extends JpaRepository<UserEntraMembership, Long> {
    Optional<UserEntraMembership> findByUserEntraIdAndResourceEntraId(UUID userEntraId, UUID resourceEntraId);

    List<UserEntraMembership> findAllByEntraStatus(EntraStatus entraStatus);

    List<UserEntraMembership> findAllByEntraStatusAndUserEntraId(EntraStatus entraStatus, UUID userEntraId);
}
