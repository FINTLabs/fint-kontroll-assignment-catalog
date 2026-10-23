package no.fintlabs.assignment.user.entra;

import no.fintlabs.assignment.user.projection.FlattenedAssignment;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserEntraMembershipRepository extends JpaRepository<UserEntraMembership, Long> {
    Optional<UserEntraMembership> findByUserEntraIdAndResourceEntraId(UUID userEntraId, UUID resourceEntraId);

    boolean existsByUserEntraIdAndResourceEntraIdAndMembershipStatus(UUID userEntraId, UUID resourceEntraId, MembershipStatus membershipStatus);

    List<UserEntraMembership> findAllByEntraStatus(EntraMembershipSyncStatus entraStatus);

    List<UserEntraMembership> findAllByEntraStatusAndUserEntraId(EntraMembershipSyncStatus entraStatus, UUID userEntraId);

    @Query("""
            SELECT DISTINCT membership
            FROM UserEntraMembership membership
            JOIN membership.flattenedAssignments assignment
            WHERE assignment.assignmentTerminationDate IS NULL
            AND membership.id > :lastProcessedId
            ORDER BY membership.id ASC
            """)
    List<UserEntraMembership> findActiveMembershipsByIdGreaterThanOrderByIdAsc(
            @Param("lastProcessedId") Long lastProcessedId,
            Pageable pageable);

    @Query("""
            SELECT membership
            FROM UserEntraMembership membership
            WHERE membership.entraStatus IN :entraStatuses
            AND membership.id > :lastProcessedId
            ORDER BY membership.id ASC
            """)
    List<UserEntraMembership> findByEntraStatusInAndIdGreaterThanOrderByIdAsc(
            @Param("entraStatuses") List<EntraMembershipSyncStatus> entraStatuses,
            @Param("lastProcessedId") Long lastProcessedId,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT membership
            FROM FlattenedAssignment assignment
            JOIN assignment.userEntraMembership membership
            WHERE assignment.assignmentId = :assignmentId
            AND membership.entraStatus IN :entraStatuses
            AND membership.id > :lastProcessedId
            ORDER BY membership.id ASC
            """)
    List<UserEntraMembership> findByAssignmentIdAndEntraStatusInAndIdGreaterThanOrderByIdAsc(
            @Param("assignmentId") Long assignmentId,
            @Param("entraStatuses") List<EntraMembershipSyncStatus> entraStatuses,
            @Param("lastProcessedId") Long lastProcessedId,
            Pageable pageable);

    @Query("""
            SELECT COUNT(DISTINCT membership.id)
            FROM UserEntraMembership membership
            JOIN membership.flattenedAssignments assignment
            WHERE membership.resourceEntraId = :resourceEntraId
            AND membership.membershipStatus = :membershipStatus
            """)
    long countByResourceEntraIdAndMembershipStatus(
            @Param("resourceEntraId") UUID resourceEntraId,
            @Param("membershipStatus") MembershipStatus membershipStatus);
}
