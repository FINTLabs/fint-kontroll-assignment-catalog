package no.fintlabs.device.assignment;

import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationService;
import no.fintlabs.applicationresourcelocation.NearestResourceLocationDto;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.assignment.exception.AssignmentException;
import no.fintlabs.device.group.DeviceGroup;
import no.fintlabs.device.group.DeviceGroupRepository;
import no.fintlabs.enforcement.LicenseEnforcementService;
import no.fintlabs.exception.ConflictException;
import no.fintlabs.exception.ResourceNotFoundException;
import no.fintlabs.opa.OpaService;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.user.User;
import no.fintlabs.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceAssignmentServiceTest {

    @Mock private DeviceGroupRepository deviceGroupRepository;
    @Mock private UserRepository userRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private LicenseEnforcementService licenseEnforcementService;
    @Mock private FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;
    @Mock private ApplicationResourceLocationService applicationResourceLocationService;
    @Mock private OpaService opaService;

    @InjectMocks
    private DeviceAssignmentService deviceAssignmentService;

    @Captor
    private ArgumentCaptor<Assignment> assignmentCaptor;

    private DeviceGroup group(long id) {
        return DeviceGroup.builder().id(id).build();
    }

    private Resource resource(long id, String name, UUID azureGroupId) {
        Resource r = new Resource();
        r.setResourceId(String.valueOf(id));
        r.setResourceName(name);
        r.setIdentityProviderGroupObjectId(azureGroupId);
        return r;
    }

    // -------- createNewAssignment --------

    @Test
    void createNewAssignment_shouldThrowNotFound_whenDeviceGroupMissing() {
        when(deviceGroupRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                deviceAssignmentService.createNewAssignment(1L, "ou-1", 100L)
        );

        verify(assignmentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(licenseEnforcementService);
    }

    @Test
    void createNewAssignment_shouldThrowNotFound_whenResourceMissing() {
        when(deviceGroupRepository.findById(100L)).thenReturn(Optional.of(group(100L)));
        when(resourceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                deviceAssignmentService.createNewAssignment(1L, "ou-1", 100L)
        );

        verify(assignmentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(licenseEnforcementService);
    }

    @Test
    void createNewAssignment_shouldThrowUnprocessable_whenResourceHasNoAzureGroupId() {
        when(deviceGroupRepository.findById(100L)).thenReturn(Optional.of(group(100L)));

        Resource r = resource(1L, "R", null);
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(r));

        AssignmentException ex = assertThrows(AssignmentException.class, () ->
                deviceAssignmentService.createNewAssignment(1L, "ou-1", 100L)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        verify(assignmentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(licenseEnforcementService);
    }

    @Test
    void createNewAssignment_shouldThrowConflict_whenDuplicateAssignmentExists() {
        when(deviceGroupRepository.findById(100L)).thenReturn(Optional.of(group(100L)));

        Resource r = resource(1L, "R", UUID.randomUUID());
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(r));

        when(assignmentRepository.findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(100L, 1L))
                .thenReturn(Optional.of(new Assignment()));

        assertThrows(ConflictException.class, () ->
                deviceAssignmentService.createNewAssignment(1L, "ou-1", 100L)
        );

        verify(assignmentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(licenseEnforcementService);
    }

    @Test
    void createNewAssignment_shouldThrowConflict_whenLicenseIncrementFails() {
        when(opaService.getUserNameAuthenticatedUser()).thenReturn("dev-user");
        when(deviceGroupRepository.findById(100L)).thenReturn(Optional.of(group(100L)));

        UUID azureGroupId = UUID.randomUUID();
        Resource r = resource(1L, "Resource A", azureGroupId);
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(r));

        when(assignmentRepository.findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(100L, 1L))
                .thenReturn(Optional.empty());

        when(applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(eq(1L), eq("ou-1")))
                .thenReturn(Optional.empty());

        when(licenseEnforcementService.incrementAssignedLicensesWhenNewAssignment(any(Assignment.class)))
                .thenReturn(false);

        assertThrows(ConflictException.class, () ->
                deviceAssignmentService.createNewAssignment(1L, "ou-1", 100L)
        );

        verify(licenseEnforcementService).incrementAssignedLicensesWhenNewAssignment(any(Assignment.class));
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void createNewAssignment_shouldSaveAssignment_andSetNearestLocation_whenPresent() {
        when(opaService.getUserNameAuthenticatedUser()).thenReturn("dev-user");
        when(deviceGroupRepository.findById(100L)).thenReturn(Optional.of(group(100L)));

        UUID azureGroupId = UUID.randomUUID();
        Resource r = resource(1L, "Resource A", azureGroupId);
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(r));

        when(assignmentRepository.findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(100L, 1L))
                .thenReturn(Optional.empty());

        NearestResourceLocationDto nearest = mock(NearestResourceLocationDto.class);
        when(nearest.orgUnitId()).thenReturn("nearest-ou");
        when(nearest.orgUnitName()).thenReturn("Nearest OU Name");

        when(applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(eq(1L), eq("ou-1")))
                .thenReturn(Optional.of(nearest));

        when(licenseEnforcementService.incrementAssignedLicensesWhenNewAssignment(any(Assignment.class)))
                .thenReturn(true);

        when(assignmentRepository.saveAndFlush(any(Assignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Assignment saved = deviceAssignmentService.createNewAssignment(1L, "ou-1", 100L);

        assertNotNull(saved);
        assertEquals("dev-user", saved.getAssignerUserName());
        assertEquals(1L, saved.getResourceRef());
        assertEquals("Resource A", saved.getResourceName());
        assertEquals("ou-1", saved.getOrganizationUnitId());
        assertEquals(azureGroupId, saved.getAzureAdGroupId());
        assertEquals(100L, saved.getDeviceGroupRef());
        assertEquals("nearest-ou", saved.getApplicationResourceLocationOrgUnitId());
        assertEquals("Nearest OU Name", saved.getApplicationResourceLocationOrgUnitName());

        verify(licenseEnforcementService).incrementAssignedLicensesWhenNewAssignment(any(Assignment.class));
        verify(assignmentRepository).saveAndFlush(any(Assignment.class));
    }

    @Test
    void createNewAssignment_shouldSaveAssignment_withoutNearestLocation_whenEmptyOptional() {
        when(opaService.getUserNameAuthenticatedUser()).thenReturn("dev-user");
        when(deviceGroupRepository.findById(100L)).thenReturn(Optional.of(group(100L)));

        UUID azureGroupId = UUID.randomUUID();
        Resource r = resource(1L, "Resource A", azureGroupId);
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(r));

        when(assignmentRepository.findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(100L, 1L))
                .thenReturn(Optional.empty());

        when(applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(eq(1L), eq("ou-1")))
                .thenReturn(Optional.empty());

        when(licenseEnforcementService.incrementAssignedLicensesWhenNewAssignment(any(Assignment.class)))
                .thenReturn(true);

        when(assignmentRepository.saveAndFlush(any(Assignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Assignment saved = deviceAssignmentService.createNewAssignment(1L, "ou-1", 100L);

        assertNotNull(saved);
        assertNull(saved.getApplicationResourceLocationOrgUnitId());
        assertNull(saved.getApplicationResourceLocationOrgUnitName());

        verify(licenseEnforcementService).incrementAssignedLicensesWhenNewAssignment(any(Assignment.class));
        verify(assignmentRepository).saveAndFlush(any(Assignment.class));
    }

    // -------- read methods --------

    @Test
    void getAllActiveAssignments_shouldDelegateToRepository() {
        List<Assignment> expected = List.of(new Assignment(), new Assignment());
        when(assignmentRepository.findAllByDeviceGroupRefIsNotNullAndAssignmentRemovedDateIsNull()).thenReturn(expected);

        List<Assignment> result = deviceAssignmentService.getAllActiveAssignments();

        assertSame(expected, result);
        verify(assignmentRepository).findAllByDeviceGroupRefIsNotNullAndAssignmentRemovedDateIsNull();
    }

    @Test
    void getActiveAssignmentsByResource_shouldDelegateToRepository() {
        List<Assignment> expected = List.of(new Assignment());
        when(assignmentRepository.findActiveDeviceAssignmentsByResourceRef(1L)).thenReturn(expected);

        List<Assignment> result = deviceAssignmentService.getActiveAssignmentsByResource(1L);

        assertSame(expected, result);
        verify(assignmentRepository).findActiveDeviceAssignmentsByResourceRef(1L);
    }

    @Test
    void getAssignmentById_shouldDelegateToRepository() {
        Assignment assignment = new Assignment();
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        Optional<Assignment> result = deviceAssignmentService.getAssignmentById(10L);

        assertTrue(result.isPresent());
        assertSame(assignment, result.get());
        verify(assignmentRepository).findById(10L);
    }

    // -------- deleteAssignment --------

    @Test
    void deleteAssignment_shouldThrowNotFound_whenAssignmentMissing() {
        when(opaService.getUserNameAuthenticatedUser()).thenReturn("dev-user");
        when(assignmentRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> deviceAssignmentService.deleteAssignment(10L));

        verify(assignmentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(flattenedDeviceAssignmentService);
        verifyNoInteractions(licenseEnforcementService);
    }

    @Test
    void deleteAssignment_shouldSetRemovedDate_andAssignerRemoveRef_whenUserFound_andDeleteFlattenedAssignments_andDecreaseLicenses() {
        when(opaService.getUserNameAuthenticatedUser()).thenReturn("dev-user");

        Assignment assignment = new Assignment();
        assignment.setId(10L);
        assertNull(assignment.getAssignmentRemovedDate());

        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        User user = User.builder().id(123L).build();
        when(userRepository.getUserByUserName("dev-user")).thenReturn(Optional.of(user));

        when(assignmentRepository.saveAndFlush(any(Assignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        deviceAssignmentService.deleteAssignment(10L);

        verify(assignmentRepository).saveAndFlush(assignmentCaptor.capture());
        Assignment saved = assignmentCaptor.getValue();

        assertNotNull(saved.getAssignmentRemovedDate());
        assertEquals(123L, saved.getAssignerRemoveRef());

        verify(licenseEnforcementService).decreaseAssignedResourcesWhenAssignmentRemoved(saved);

        verify(flattenedDeviceAssignmentService)
                .deleteFlattenedDeviceAssignments(eq(saved), eq("Associated assignment terminated by user"));
    }

    @Test
    void deleteAssignment_shouldStillDeleteFlattenedAssignments_whenAuthenticatedUsernameEmpty_andDecreaseLicenses() {
        when(opaService.getUserNameAuthenticatedUser()).thenReturn("");

        Assignment assignment = new Assignment();
        assignment.setId(10L);

        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.saveAndFlush(any(Assignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        deviceAssignmentService.deleteAssignment(10L);

        verify(userRepository, never()).getUserByUserName(anyString());

        verify(licenseEnforcementService).decreaseAssignedResourcesWhenAssignmentRemoved(assignment);

        verify(flattenedDeviceAssignmentService)
                .deleteFlattenedDeviceAssignments(eq(assignment), eq("Associated assignment terminated by user"));
    }

    @Test
    void deleteAssignment_shouldNotSetAssignerRemoveRef_whenUserNotFound() {
        when(opaService.getUserNameAuthenticatedUser()).thenReturn("dev-user");

        Assignment assignment = new Assignment();
        assignment.setId(10L);

        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(userRepository.getUserByUserName("dev-user")).thenReturn(Optional.empty());
        when(assignmentRepository.saveAndFlush(any(Assignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        deviceAssignmentService.deleteAssignment(10L);

        verify(assignmentRepository).saveAndFlush(assignmentCaptor.capture());
        Assignment saved = assignmentCaptor.getValue();

        assertNotNull(saved.getAssignmentRemovedDate());
        assertNull(saved.getAssignerRemoveRef());

        verify(licenseEnforcementService).decreaseAssignedResourcesWhenAssignmentRemoved(saved);
        verify(flattenedDeviceAssignmentService)
                .deleteFlattenedDeviceAssignments(eq(saved), eq("Associated assignment terminated by user"));
    }
}
