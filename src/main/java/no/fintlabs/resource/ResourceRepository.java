package no.fintlabs.resource;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository  extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Resource r where r.id = :id")
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-1"))
    Optional<Resource> lockByResourceId(@Param("id") Long id);

    @Query("select r from Resource r where r.status = 'ACTIVE'")
    List<Resource> findByStatusACTIVE();



    @Modifying
    @Transactional
    @Query("UPDATE Resource r SET r.numberOfResourcesAssigned = null")
    void clearNumberOfResourcesAssignedInResources();


}
