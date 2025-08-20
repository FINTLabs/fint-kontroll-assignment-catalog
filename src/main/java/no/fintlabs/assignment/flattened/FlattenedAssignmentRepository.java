package no.fintlabs.assignment.flattened;

import jakarta.persistence.QueryHint;
import no.fintlabs.reporting.FlattenedAssignmentReport;
import org.hibernate.jpa.AvailableHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface FlattenedAssignmentRepository extends JpaRepository<FlattenedAssignment, Long>, JpaSpecificationExecutor<FlattenedAssignment> {
    List<FlattenedAssignment> findByIdentityProviderGroupMembershipConfirmedAndAssignmentTerminationDateIsNull(boolean identityProviderGroupMembershipConfirmed);

    List<FlattenedAssignment> findByAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmedFalse();

    List<FlattenedAssignment> findByAssignmentId(Long assignmentId);

    List<FlattenedAssignment> findByAssignmentIdAndAssignmentTerminationDateIsNull(Long assignmentId);

    Optional<FlattenedAssignment> findByUserRefAndResourceRefAndAssignmentTerminationDateIsNull(Long userRef, Long resourceRef);

    List<FlattenedAssignment> findByAssignmentViaRoleRefNotAndUserRefAndResourceRefAndAssignmentTerminationDateIsNull(Long id, Long userRef, Long resourceRef);

    List<FlattenedAssignment> findByAssignmentIdAndUserRefAndAssignmentViaRoleRefAndAssignmentTerminationDateIsNull(Long assignmentId, Long userRef, Long resourceRef);

    @Query("SELECT fa.id FROM FlattenedAssignment fa WHERE fa.userRef = :userId AND fa.assignmentViaRoleRef = :roleId AND fa.assignmentTerminationDate IS NULL")
    List<Long> findFlattenedAssignmentIdsByUserAndRoleRef(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Query("SELECT fa, u, a, r, assignerUser.firstName, assignerUser.lastName " +
           "FROM FlattenedAssignment fa " +
           "LEFT JOIN User u ON u.id = fa.userRef " +
           "LEFT JOIN Role r ON r.id = fa.assignmentViaRoleRef " +
           "LEFT JOIN Assignment a ON a.id = fa.assignmentId " +
           "LEFT JOIN User assignerUser ON assignerUser.userName = a.assignerUserName " +
           "WHERE fa.resourceRef = :resourceId " +
           "AND fa.assignmentTerminationDate IS NULL " +
           "AND (:userType = 'ALLTYPES' OR LOWER(u.userType) = LOWER(:userType)) " +
           "AND (:search IS NULL OR LOWER(u.firstName) LIKE %:search% OR LOWER(u.lastName) LIKE %:search%) " +
           "AND (:orgUnits IS NULL OR u.organisationUnitId IN :orgUnits)")
    Page<Object[]> findAssignmentsByResourceAndUserTypeAndSearch(@Param("resourceId") Long resourceId, @Param("userType") String userType, @Param("orgUnits") List<String> orgUnits,
                                                                 @Param("search") String search, Pageable pageable);

    @Query("SELECT fa, res, u, a, r, assignerUser.firstName, assignerUser.lastName, 'user' as objectType  " +
            "FROM FlattenedAssignment fa " +
            "LEFT JOIN User u ON u.id = fa.userRef " +
            "LEFT JOIN Role r ON r.id = fa.assignmentViaRoleRef " +
            "LEFT JOIN Assignment a ON a.id = fa.assignmentId " +
            "LEFT JOIN Resource res ON res.id = fa.resourceRef " +
            "LEFT JOIN User assignerUser ON assignerUser.userName = a.assignerUserName " +
            "WHERE fa.resourceRef = :resourceId " +
            "AND fa.assignmentTerminationDate IS NULL " +
            "AND (:userType = 'ALLTYPES' OR LOWER(u.userType) = LOWER(:userType)) " +
            "AND (:fullName IS NULL OR LOWER(u.firstName) LIKE %:fullName% " +
            "OR LOWER(u.firstName) LIKE %:firstName% AND LOWER(u.lastName) LIKE %:lastName% " +
            "OR :firstName IS NULL AND LOWER(u.lastName) LIKE %:fullName%) " +
            "AND (:orgUnits IS NULL OR u.organisationUnitId IN :orgUnits) " +
            "AND (:userIds IS NULL OR fa.userRef IN :userIds) " +
            "ORDER BY u.firstName, u.lastName"
    )
    Optional<Page<Object[]>> findAssignmentsByResourceAndUserTypeAndNamesSearch(
            @Param("resourceId") Long resourceId,
            @Param("userType") String userType,
            @Param("orgUnits") List<String> orgUnits,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("fullName") String fullName,
            @Param("userIds") List<Long> userIds, Pageable pageable
    );

    @Query("SELECT fa, res, r, u, a, assignerUser.firstName, assignerUser.lastName, 'role' as objectType " +
           "FROM FlattenedAssignment fa " +
           "LEFT JOIN User u ON u.id = fa.userRef " +
           "LEFT JOIN Role r ON r.id = fa.assignmentViaRoleRef " +
           "LEFT JOIN Assignment a ON a.id = fa.assignmentId " +
           "LEFT JOIN Resource res ON res.id = fa.resourceRef " +
           "LEFT JOIN User assignerUser ON assignerUser.userName = a.assignerUserName " +
           "WHERE fa.assignmentViaRoleRef = :roleId " +
           "AND (fa.assignmentTerminationDate IS NULL) " +
           "AND (a.assignmentRemovedDate IS NULL) " +
           "AND (:resourceType = 'ALLTYPES' OR LOWER(res.resourceType) = LOWER(:resourceType)) " +
           "AND (:search IS NULL OR LOWER(res.resourceName) LIKE %:search%) " +
           "AND (:resourceIds IS NULL OR fa.resourceRef IN :resourceIds) " +
           "ORDER BY res.resourceName ASC")
    Page<Object[]> findAssignmentsByRoleAndResourceTypeAndSearch(
            @Param("roleId") Long roleId,
            @Param("resourceType") String resourceType,
            @Param("resourceIds") List<Long> resourceIds,
            @Param("search") String search, Pageable pageable);

    @Query("SELECT fa, res, r, u, a, assignerUser.firstName, assignerUser.lastName, 'user' as objectType  FROM FlattenedAssignment fa " +
           "LEFT JOIN User u ON u.id = fa.userRef " +
           "LEFT JOIN Role r ON r.id = fa.assignmentViaRoleRef " +
           "LEFT JOIN Assignment a ON a.id = fa.assignmentId " +
           "LEFT JOIN Resource res ON res.id = fa.resourceRef " +
           "LEFT JOIN User assignerUser ON assignerUser.userName = a.assignerUserName " +
           "WHERE fa.userRef = :userId " +
           "AND (fa.assignmentTerminationDate IS NULL) " +
           "AND (a.assignmentRemovedDate IS NULL) " +
           "AND (:resourceType = 'ALLTYPES' OR LOWER(res.resourceType) = LOWER(:resourceType)) " +
           "AND (:search IS NULL OR LOWER(res.resourceName) LIKE %:search%) " +
           "AND (:resourceIds IS NULL OR fa.resourceRef IN :resourceIds) " +
           "ORDER BY res.resourceName ASC")
    Page<Object[]> findAssignmentsByUserAndResourceTypeAndSearch(
            @Param("userId") Long userId,
            @Param("resourceType") String resourceType,
            @Param("resourceIds") List<Long> resourceIds,
            @Param("search") String search, Pageable pageable);

    List<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(UUID identityProviderGroupObjectId, UUID identityProviderUserObjectId);

    List<FlattenedAssignment> findByIdentityProviderGroupMembershipConfirmedAndAssignmentTerminationDateIsNullAndAssignmentId(boolean groupMembershipConfirmed, Long assignmentId);

    List<FlattenedAssignment> findByAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmedFalseAndAssignmentId(Long assignmentId);

    @Query("SELECT fa.id FROM FlattenedAssignment fa WHERE fa.identityProviderUserObjectId IS NULL AND fa.assignmentTerminationDate IS NULL")
    List<Long> findIdsWhereIdentityProviderUserObjectIdIsNull();

    @QueryHints({
            @QueryHint(name = AvailableHints.HINT_FETCH_SIZE, value = "5000"),
            @QueryHint(name = AvailableHints.HINT_READ_ONLY, value = "true"),
            @QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "false")
    })
    @Query("SELECT new no.fintlabs.reporting.FlattenedAssignmentReport(fa.id, CAST(fa.resourceRef AS string), res.resourceName, u.organisationUnitId, u.organisationUnitName, u.userType, a.applicationResourceLocationOrgUnitId, a.applicationResourceLocationOrgUnitName, fa.assignmentCreationDate, fa.assignmentTerminationDate) " +
           "FROM FlattenedAssignment fa " +
           "LEFT JOIN User u ON u.id = fa.userRef " +
           "LEFT JOIN Assignment a ON a.id = fa.assignmentId " +
           "LEFT JOIN Resource res ON res.id = fa.resourceRef ")
    Stream<FlattenedAssignmentReport> streamAllFlattenedAssignmentsForReport();
}
