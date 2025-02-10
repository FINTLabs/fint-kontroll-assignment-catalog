package no.fintlabs.applicationresourcelocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ApplicationResourceLocationRepository extends JpaRepository<ApplicationResourceLocation,Long>{

    @Query(value="select arl.orgunitid from application_resource_location arl join orgunit_distance od on arl.orgunitid = od.orgunitid" +
            " where arl.applicationresourceid = ?1 and od.suborgunitid = ?2" +
            " order by od.distance limit 1"
            , nativeQuery = true)
    Optional< String> findNearestResourceConsumerForOrgUnit(Long resourceRef, String orgUnitId);

    Optional<ApplicationResourceLocation> findByResourceIdAndOrgUnitId(String resourceId, String orgUnitId);
}
