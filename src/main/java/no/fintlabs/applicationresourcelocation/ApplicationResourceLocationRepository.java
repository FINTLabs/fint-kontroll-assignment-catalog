package no.fintlabs.applicationresourcelocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplicationResourceLocationRepository extends JpaRepository<ApplicationResourceLocation, Long> {

    @Query("SELECT new no.fintlabs.applicationresourcelocation.NearestResourceLocationDto(arl.orgUnitId, arl.orgUnitName) " +
           "FROM ApplicationResourceLocation arl " +
           "JOIN OrgUnitDistance od ON arl.orgUnitId = od.orgUnitId " +
           "WHERE arl.applicationResourceId = :resourceRef AND od.subOrgUnitId = :orgUnitId " +
           "ORDER BY od.distance ASC")
    List<NearestResourceLocationDto> findNearestApplicationResourceLocationForOrgUnit(@Param("resourceRef") Long resourceRef,
                                                                                      @Param("orgUnitId") String orgUnitId);
}
