package no.fintlabs.applicationresourcelocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    Optional<ApplicationResourceLocation> findByApplicationResourceIdAndOrgUnitId(Long applicationResourceId, String orgUnitId);


    @Query("SELECT new no.fintlabs.applicationresourcelocation.NearestResourceLocationDto(arl.orgUnitId, arl.orgUnitName) " +
            "FROM ApplicationResourceLocation arl " +
            "JOIN OrgUnitDistance od ON arl.orgUnitId = od.orgUnitId " +
            "WHERE arl.applicationResourceId = :resourceRef AND od.subOrgUnitId = :orgUnitId " +
            "ORDER BY od.distance ASC")
    List<NearestResourceLocationDto> findNearestApplicationResourceLocationForOrgUnit(@Param("resourceRef") Long resourceRef,
                                                                                      @Param("orgUnitId") String orgUnitId);


}


