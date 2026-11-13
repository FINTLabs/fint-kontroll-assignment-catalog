package no.fintlabs.applicationresourcelocation;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplicationResourceLocationRepository extends JpaRepository<ApplicationResourceLocation, Long> {

    @Query("select a from ApplicationResourceLocation a where a.applicationResourceId = ?1 and a.orgUnitId = ?2")
    List<ApplicationResourceLocation> findByApplicationResourceIdAndOrgUnitId(Long applicationResourceId, String orgUnitId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select arl
            from ApplicationResourceLocation arl
            where arl.applicationResourceId = :resourceId
            """)
    List<ApplicationResourceLocation> lockByApplicationResourceId(@Param("resourceId") Long resourceId);


    @Query("SELECT new no.fintlabs.applicationresourcelocation.NearestResourceLocationDto(arl.orgUnitId, arl.orgUnitName) " +
            "FROM ApplicationResourceLocation arl " +
            "JOIN OrgUnitDistance od ON arl.orgUnitId = od.orgUnitId " +
            "WHERE arl.applicationResourceId = :resourceRef AND od.subOrgUnitId = :orgUnitId " +
            "ORDER BY od.distance ASC")
    List<NearestResourceLocationDto> findNearestApplicationResourceLocationForOrgUnit(@Param("resourceRef") Long resourceRef,
                                                                                      @Param("orgUnitId") String orgUnitId);

}
