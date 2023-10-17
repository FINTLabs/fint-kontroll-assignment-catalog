package no.fintlabs.role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query("""
    select r from Role r join r.assignments assignments where assignments.resourceRef = ?1
    """)
    List<Role> getRolesByResourceId(Long id);

}
