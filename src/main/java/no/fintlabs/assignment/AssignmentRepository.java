package no.fintlabs.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long>, JpaSpecificationExecutor<Assignment> {

    Optional<Assignment> findAssignmentByUserRefAndResourceRefAndAssignmentRemovedDateIsNull(Long userRef, Long resourceRef);

    Optional<Assignment> findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(Long roleRef, Long resourceRef);

    List<Assignment> findAssignmentsByRoleRefAndAssignmentRemovedDateIsNull(Long roleId);

    List<Assignment> findAssignmentsByUserRefAndAssignmentRemovedDateIsNull(Long userId);
}
