package no.fintlabs.applicationresourcelocation;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ApplicationResourceLocationRepository extends JpaRepository<ApplicationResourceLocation,Long>{

    @Query(value="select arl.orgunitid from application_resource_location arl join orgunit_distance od on arl.orgunitid = od.orgunitid" +
            " where arl.applicationresourceid = ?1 and od.suborgunitid = ?2" +
            " order by od.distance limit 1"
            , nativeQuery = true)
    Optional< String> findNearestResourceConsumerForOrgUnit(Long resourceRef, String orgUnitId);

    Optional<ApplicationResourceLocation> findByResourceIdAndOrgUnitId(String resourceId, String orgUnitId);

    @Query("select a from ApplicationResourceLocation a where a.applicationResourceId = ?1 and a.orgUnitId = ?2")
    List<ApplicationResourceLocation> findByApplicationResourceIdAndOrgUnitId(Long applicationResourceId, String orgUnitId);


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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select arl
        from ApplicationResourceLocation arl
        where arl.applicationResourceId = :resourceId and arl.orgUnitId = :orgUnitId
    """)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-1"))
    List<ApplicationResourceLocation> lockByResourceAndOrgUnit(@Param("resourceId") Long resourceId,
                                                                   @Param("orgUnitId") String orgUnitId);

}


