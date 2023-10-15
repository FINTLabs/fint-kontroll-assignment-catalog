package no.fintlabs.resource;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ResourceRepository  extends JpaRepository<Resource, Long>{
    @Query("""
    select r from Resource r join r.assignments assignments where assignments.userRef = ?1
    """)
    List<Resource> getResourcesByUserId(Long id);
}
