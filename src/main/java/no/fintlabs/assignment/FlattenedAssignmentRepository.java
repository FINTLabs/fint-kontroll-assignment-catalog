package no.fintlabs.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FlattenedAssignmentRepository extends JpaRepository<FlattenedAssignment, Long>, JpaSpecificationExecutor<FlattenedAssignment> {
}
