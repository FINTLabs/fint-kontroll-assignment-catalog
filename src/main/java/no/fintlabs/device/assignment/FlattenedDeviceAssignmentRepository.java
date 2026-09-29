package no.fintlabs.device.assignment;

import jakarta.persistence.QueryHint;
import no.fintlabs.reporting.FlattenedAssignmentReport;
import org.hibernate.jpa.AvailableHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Repository
public interface FlattenedDeviceAssignmentRepository extends JpaRepository<FlattenedDeviceAssignment, Long>, JpaSpecificationExecutor<FlattenedDeviceAssignment> {
    List<FlattenedDeviceAssignment> findByAssignmentIdAndTerminationDateIsNull(Long assignmentId);


    List<FlattenedDeviceAssignment> findByDeviceRefAndAssignmentViaGroupRefAndTerminationDateIsNull(Long deviceRef, Long deviceGroupRef);


    List<FlattenedDeviceAssignment> findByDeviceRefAndTerminationDateIsNull(Long deviceRef);

    @QueryHints({
            @QueryHint(name = AvailableHints.HINT_FETCH_SIZE, value = "5000"),
            @QueryHint(name = AvailableHints.HINT_READ_ONLY, value = "true"),
            @QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "false")
    })
    @Query("SELECT new no.fintlabs.reporting.FlattenedAssignmentReport(fa.id, CAST(fa.resourceRef AS string), res.resourceName, u.organisationUnitId, u.organisationUnitName, u.userType, a.applicationResourceLocationOrgUnitId, a.applicationResourceLocationOrgUnitName, fa.assignmentCreationDate, fa.assignmentTerminationDate) " +
           "FROM FlattenedAssignment fa " +
           "LEFT JOIN User u ON u.id = fa.userRef " +
           "LEFT JOIN Assignment a ON a.id = fa.assignmentId " +
           "LEFT JOIN Resource res ON res.id = fa.resourceRef ")
    Stream<FlattenedAssignmentReport> streamAllFlattenedAssignmentsForReport();
}
