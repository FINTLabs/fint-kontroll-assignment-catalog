package no.fintlabs.assignment.flattened;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlattenedAssignmentRepository extends JpaRepository<FlattenedAssignment, Long>, JpaSpecificationExecutor<FlattenedAssignment> {

    List<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndIdentityProviderGroupMembershipConfirmedAndAssignmentTerminationDateIsNull(
            UUID identityProviderGroupObjectId, UUID identityProviderUserObjectId, boolean identityProviderGroupMembershipConfirmed);

    List<FlattenedAssignment> findByIdentityProviderGroupMembershipConfirmedAndAssignmentTerminationDateIsNull(boolean identityProviderGroupMembershipConfirmed);

    List<FlattenedAssignment> findByAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmedFalse();

    List<FlattenedAssignment> findByAssignmentId(Long assignmentId);

    Optional<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndAssignmentIdAndAssignmentTerminationDateIsNull(UUID identityProviderGroupObjectId,
                                                                                                                                                      UUID identityProviderUserObjectId,
                                                                                                                                                      Long assignmentId);

    List<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndAssignmentId(UUID identityProviderGroupObjectId, UUID identityProviderUserObjectId,
                                                                                                                Long assignmentId);
    Optional<FlattenedAssignment> findByUserRefAndResourceRefAndAssignmentTerminationDateIsNull(Long userRef, Long resourceRef);

    Optional<FlattenedAssignment> findByAssignmentIdAndUserRefAndAssignmentViaRoleRefAndAssignmentTerminationDateIsNull(Long assignmentId, Long userRef, Long resourceRef);

    List<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmed(
            UUID identityProviderGroupObjectId, UUID identityProviderUserObjectId, boolean groupMembershipDeletionConfirmed);

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

    @Query("SELECT fa, res, r, u, a, assignerUser.firstName, assignerUser.lastName FROM FlattenedAssignment fa " +
           "LEFT JOIN User u ON u.id = fa.userRef " +
           "LEFT JOIN Role r ON r.id = fa.assignmentViaRoleRef " +
           "LEFT JOIN Assignment a ON a.id = fa.assignmentId " +
           "LEFT JOIN Resource res ON res.id = fa.resourceRef " +
           "LEFT JOIN User assignerUser ON assignerUser.userName = a.assignerUserName " +
           "WHERE fa.assignmentViaRoleRef = :roleId " +
           "AND (fa.assignmentTerminationDate IS NULL) " +
           "AND (a.assignmentRemovedDate IS NULL) " +
           "AND (:resourceType = 'ALLTYPES' OR LOWER(res.resourceType) = LOWER(:resourceType)) " +
           "AND (:search IS NULL OR LOWER(res.resourceName) LIKE %:search%)")
    Page<Object[]> findAssignmentsByRoleAndResourceTypeAndSearch(@Param("roleId") Long roleId, @Param("resourceType") String resourceType, @Param("search") String search, Pageable pageable);

    @Query("SELECT fa, res, r, u, a, assignerUser.firstName, assignerUser.lastName FROM FlattenedAssignment fa " +
           "LEFT JOIN User u ON u.id = fa.userRef " +
           "LEFT JOIN Role r ON r.id = fa.assignmentViaRoleRef " +
           "LEFT JOIN Assignment a ON a.id = fa.assignmentId " +
           "LEFT JOIN Resource res ON res.id = fa.resourceRef " +
           "LEFT JOIN User assignerUser ON assignerUser.userName = a.assignerUserName " +
           "WHERE fa.userRef = :userId " +
           "AND (fa.assignmentTerminationDate IS NULL) " +
           "AND (a.assignmentRemovedDate IS NULL) " +
           "AND (:resourceType = 'ALLTYPES' OR LOWER(res.resourceType) = LOWER(:resourceType)) " +
           "AND (:search IS NULL OR LOWER(res.resourceName) LIKE %:search%)")
    Page<Object[]> findAssignmentsByUserAndResourceTypeAndSearch(@Param("userId") Long userId, @Param("resourceType") String resourceType, @Param("search") String search, Pageable pageable);

    List<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(UUID identityProviderGroupObjectId, UUID identityProviderUserObjectId);

    List<FlattenedAssignment> findByIdentityProviderGroupMembershipConfirmedAndAssignmentTerminationDateIsNullAndAssignmentId(boolean groupMembershipConfirmed, Long assignmentId);

    List<FlattenedAssignment> findByAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmedFalseAndAssignmentId(Long assignmentId);

    @Query("SELECT fa.id FROM FlattenedAssignment fa WHERE fa.identityProviderUserObjectId IS NULL AND fa.assignmentTerminationDate IS NULL")
    List<Long> findIdsWhereIdentityProviderUserObjectIdIsNull();
}
