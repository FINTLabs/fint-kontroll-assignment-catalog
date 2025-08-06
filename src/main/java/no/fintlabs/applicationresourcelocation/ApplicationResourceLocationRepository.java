package no.fintlabs.applicationresourcelocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ApplicationResourceLocationRepository extends JpaRepository<ApplicationResourceLocation,Long>{

    @Query("select a from ApplicationResourceLocation a where a.applicationResourceId = ?1 and a.orgUnitId = ?2")
    Optional<List<ApplicationResourceLocation>> findByApplicationResourceIdAndOrgUnitId(Long applicationResourceId, String orgUnitId);


    @Query("SELECT new no.fintlabs.applicationresourcelocation.NearestResourceLocationDto(arl.orgUnitId, arl.orgUnitName) " +
            "FROM ApplicationResourceLocation arl " +
            "JOIN OrgUnitDistance od ON arl.orgUnitId = od.orgUnitId " +
            "WHERE arl.applicationResourceId = :resourceRef AND od.subOrgUnitId = :orgUnitId " +
            "ORDER BY od.distance ASC")
    List<NearestResourceLocationDto> findNearestApplicationResourceLocationForOrgUnit(@Param("resourceRef") Long resourceRef,
                                                                                      @Param("orgUnitId") String orgUnitId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE application_resource_location SET numberofresourcesassigned = null", nativeQuery = true)
    void clearNumberOfResourcesAssignedInLocations();


}


