package no.fintlabs.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long>, JpaSpecificationExecutor<Assignment> {
    Optional<Assignment> findAssignmentByUserRefAndResourceRef (Long userRef, Long resourceRef);

    Optional<Assignment> findAssignmentByRoleRefAndResourceRef(Long roleRef, Long resourceRef);
}
