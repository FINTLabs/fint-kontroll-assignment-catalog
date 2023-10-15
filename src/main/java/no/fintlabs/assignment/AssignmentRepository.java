package no.fintlabs.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    Assignment findAssignmentByUserRefAndResourceRef (Long userRef, Long resourceRef);
}
