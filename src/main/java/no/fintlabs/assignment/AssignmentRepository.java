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

    List<Assignment> findAssignmentsByUserRefAndAssignmentRemovedDateIsNull(Long userId);

    List<Assignment> findAssignmentsByUserRefAndAssignmentRemovedDateIsNotNull(Long userId);

    List<Assignment> findAssignmentsByResourceRefAndAssignmentRemovedDateIsNull(Long resourceId);

    @Query("SELECT a.id FROM Assignment a WHERE a.role.id = :roleId AND a.user.id = :userId AND a.assignmentRemovedDate IS NULL")
    List<Long> findAssignmentIdsByRoleRefAndUserRefAndAssignmentRemovedDateIsNull(
            @Param("roleId") Long roleId,
            @Param("userId") Long userId
    );
}
