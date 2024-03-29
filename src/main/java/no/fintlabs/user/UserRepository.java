package no.fintlabs.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Query("""
    select u from User u join u.assignments assignments where assignments.resourceRef = ?1 and u.userType = ?2
    """)
    List<User> getUsersByResourceId(Long id, String userType);

    @Query("""
    select u from User u join u.assignments assignments where assignments.resourceRef = ?1
    """)
    List<User> getUsersByResourceId(Long id);

    Optional<User> getUserByUserName(String userName);
}
