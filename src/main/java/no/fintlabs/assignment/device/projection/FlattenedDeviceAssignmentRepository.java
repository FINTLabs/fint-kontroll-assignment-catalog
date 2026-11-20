package no.fintlabs.assignment.device.projection;

import jakarta.persistence.QueryHint;
import no.fintlabs.assignment.device.entra.DeviceEntraMembership;
import no.fintlabs.entra.membership.MembershipStatus;
import no.fintlabs.reporting.FlattenedAssignmentReport;
import org.hibernate.jpa.AvailableHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Repository
public interface FlattenedDeviceAssignmentRepository extends JpaRepository<FlattenedDeviceAssignment, Long>, JpaSpecificationExecutor<FlattenedDeviceAssignment> {
    List<FlattenedDeviceAssignment> findByAssignmentIdAndTerminationDateIsNull(Long assignmentId);


    List<FlattenedDeviceAssignment> findByDeviceRefAndAssignmentViaGroupRefAndTerminationDateIsNull(Long deviceRef, Long deviceGroupRef);


    List<FlattenedDeviceAssignment> findByDeviceRefAndTerminationDateIsNull(Long deviceRef);

    @Query("""
            SELECT COUNT(DISTINCT membership.id)
            FROM FlattenedDeviceAssignment fa
            JOIN fa.deviceEntraMembership membership
            WHERE fa.resourceRef = :resourceRef
            AND fa.applicationResourceLocationOrgUnitId = :orgUnitId
            AND fa.terminationDate IS NULL
            AND membership.membershipStatus = :membershipStatus
            """)
    long countDistinctMembershipsByResourceRefAndOrgUnitIdAndMembershipStatus(
            @Param("resourceRef") Long resourceRef,
            @Param("orgUnitId") String orgUnitId,
            @Param("membershipStatus") MembershipStatus membershipStatus);
    boolean existsByDeviceEntraMembershipAndTerminationDateIsNull(DeviceEntraMembership deviceEntraMembership);

    void deleteAllByDeviceRef(Long deviceRef);
}
