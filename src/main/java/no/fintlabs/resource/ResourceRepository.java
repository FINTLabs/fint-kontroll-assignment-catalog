package no.fintlabs.resource;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Resource r where r.id = :id")
    Resource lockByResourceId(@Param("id") Long id);

    @Query("SELECT resource.id FROM Resource resource WHERE resource.id > :lastProcessedId ORDER BY resource.id ASC")
    List<Long> findIdsByIdGreaterThanOrderByIdAsc(@Param("lastProcessedId") Long lastProcessedId, Pageable pageable);

}
