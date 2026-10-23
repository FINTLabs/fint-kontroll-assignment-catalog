package no.fintlabs.device.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceGroupRepository extends JpaRepository<DeviceGroup, Long>, JpaSpecificationExecutor<DeviceGroup> {

    @Query(
            value = """
                    SELECT DISTINCT deviceGroup
                    FROM DeviceGroup deviceGroup
                    JOIN Assignment assignment ON assignment.deviceGroupRef = deviceGroup.id
                    WHERE assignment.resourceRef = :resourceId
                    AND assignment.assignmentRemovedDate IS NULL
                    AND (:search IS NULL
                        OR LOWER(deviceGroup.name) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(deviceGroup.orgUnitId) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(deviceGroup.platform) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(deviceGroup.deviceType) LIKE LOWER(CONCAT('%', :search, '%')))
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT deviceGroup.id)
                    FROM DeviceGroup deviceGroup
                    JOIN Assignment assignment ON assignment.deviceGroupRef = deviceGroup.id
                    WHERE assignment.resourceRef = :resourceId
                    AND assignment.assignmentRemovedDate IS NULL
                    AND (:search IS NULL
                        OR LOWER(deviceGroup.name) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(deviceGroup.orgUnitId) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(deviceGroup.platform) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(deviceGroup.deviceType) LIKE LOWER(CONCAT('%', :search, '%')))
                    """
    )
    Page<DeviceGroup> findAssignedToResource(
            @Param("resourceId") Long resourceId,
            @Param("search") String search,
            Pageable pageable);
}
