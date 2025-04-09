package no.fintlabs.resource;

import no.fintlabs.role.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository  extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    Resource findResourcesById(Long id);

    @Query("select r from Resource r where r.status = 'ACTIVE'")
    List<Resource> findByStatusACTIVE();


}
