package no.fintlabs.user;

import no.fintlabs.resource.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{

    @Query("""
    select u from User u join u.assignments assignments where assignments.resourceRef = ?1
    """)
    List<User> getUsersByResourceId(Long id);

}
