package no.fintlabs.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long>, JpaSpecificationExecutor<Assignment> {

    Optional<Assignment> findAssignmentByUserRefAndResourceRefAndAssignmentRemovedDateIsNull(Long userRef, Long resourceRef);

    Optional<Assignment> findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(Long roleRef, Long resourceRef);

    List<Assignment> findAssignmentsByRoleRefAndAssignmentRemovedDateIsNull(Long roleId);

    List<Assignment> findAssignmentsByDeviceGroupRefAndAssignmentRemovedDateIsNull(Long deviceGroupId);

    List<Assignment> findAssignmentsByUserRefAndAssignmentRemovedDateIsNull(Long userId);

    List<Assignment> findAssignmentsByUserRefAndAssignmentRemovedDateIsNotNull(Long userId);


    @Query("SELECT a FROM Assignment a WHERE a.resourceRef = :resourceId AND a.assignmentRemovedDate IS NULL AND" +
            "(a.userRef IS NOT NULL OR a.roleRef IS NOT NULL)")
    List<Assignment> findActiveUserAssignmentsByResourceRef(Long resourceId);

    @Query("SELECT a FROM Assignment a WHERE a.resourceRef = :resourceId AND a.assignmentRemovedDate IS NULL AND" +
            " a.deviceGroupRef IS NOT NULL")
    List<Assignment> findActiveDeviceAssignmentsByResourceRef(Long resourceId);

    List<Assignment> findAssignmentsByResourceRefAndApplicationResourceLocationOrgUnitId(Long resourceRef, String orgUnitId);

    List<Assignment> findAllByDeviceGroupRefIsNotNullAndAssignmentRemovedDateIsNull();

    List<Assignment> findAllByUserRefIsNotNullOrRoleRefIsNotNull();

    @Query("SELECT a.id FROM Assignment a WHERE a.role.id = :roleId AND a.user.id = :userId AND a.assignmentRemovedDate IS NULL")
    List<Long> findAssignmentIdsByRoleRefAndUserRefAndAssignmentRemovedDateIsNull(
            @Param("roleId") Long roleId,
            @Param("userId") Long userId
    );
}
