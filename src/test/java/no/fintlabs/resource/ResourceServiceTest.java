package no.fintlabs.resource;

import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.device.assignment.DeviceAssignmentService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceServiceTest {

    private final ResourceRepository resourceRepository = mock(ResourceRepository.class);
    private final AssignmentService assignmentService = mock(AssignmentService.class);
    private final DeviceAssignmentService deviceAssignmentService = mock(DeviceAssignmentService.class);
    private final ResourceService resourceService = new ResourceService(resourceRepository, assignmentService, deviceAssignmentService);

    @Test
    void saveSetsStatusToActiveWhenMissing() {
        Resource resource = Resource.builder()
                .id(1L)
                .build();
        when(resourceRepository.save(resource)).thenReturn(resource);

        resourceService.save(resource);

        assertThat(resource.getStatus()).isEqualTo("ACTIVE");
        verify(resourceRepository).save(resource);
        verify(assignmentService, never()).deactivateAssignmentsByResourceId(resource.getId());
        verify(deviceAssignmentService, never()).deactivateAssignmentsByResourceId(resource.getId());
    }

    @Test
    void saveSetsAssignedResourcesToZeroWhenMissing() {
        Resource resource = Resource.builder()
                .id(1L)
                .numberOfResourcesAssigned(null)
                .build();
        when(resourceRepository.save(resource)).thenReturn(resource);

        resourceService.save(resource);

        assertThat(resource.getNumberOfResourcesAssigned()).isZero();
        verify(resourceRepository).save(resource);
    }

    @Test
    void saveKeepsExistingStatus() {
        Resource resource = Resource.builder()
                .id(1L)
                .status("INACTIVE")
                .build();
        when(resourceRepository.save(resource)).thenReturn(resource);

        resourceService.save(resource);

        assertThat(resource.getStatus()).isEqualTo("INACTIVE");
        verify(resourceRepository).save(resource);
        verify(assignmentService, never()).deactivateAssignmentsByResourceId(resource.getId());
        verify(deviceAssignmentService, never()).deactivateAssignmentsByResourceId(resource.getId());
    }

    @Test
    void saveUpdatedResourceDeactivatesAssignmentsWhenStatusIsInactive() {
        Resource resource = Resource.builder()
                .id(1L)
                .status("INACTIVE")
                .build();
        when(resourceRepository.save(resource)).thenReturn(resource);

        resourceService.saveUpdatedResource(resource);

        verify(assignmentService).deactivateAssignmentsByResourceId(resource.getId());
        verify(deviceAssignmentService).deactivateAssignmentsByResourceId(resource.getId());
    }

    @Test
    void saveUpdatedResourcePreservesExistingAssignedResources() {
        Resource existingResource = Resource.builder()
                .id(1L)
                .numberOfResourcesAssigned(7L)
                .build();
        Resource incomingResource = Resource.builder()
                .id(1L)
                .numberOfResourcesAssigned(null)
                .status("ACTIVE")
                .build();

        when(resourceRepository.findById(1L)).thenReturn(java.util.Optional.of(existingResource));
        when(resourceRepository.save(incomingResource)).thenReturn(incomingResource);

        resourceService.saveUpdatedResource(incomingResource);

        assertThat(incomingResource.getNumberOfResourcesAssigned()).isEqualTo(7L);
        verify(resourceRepository).save(incomingResource);
    }

    @Test
    void saveUpdatedResourceDoesNotDeactivateAssignmentsWhenStatusIsActive() {
        Resource resource = Resource.builder()
                .id(1L)
                .status("ACTIVE")
                .build();
        when(resourceRepository.save(resource)).thenReturn(resource);

        resourceService.saveUpdatedResource(resource);

        verify(assignmentService, never()).deactivateAssignmentsByResourceId(resource.getId());
        verify(deviceAssignmentService, never()).deactivateAssignmentsByResourceId(resource.getId());
    }

    @Test
    void deactivateAssignmentsForInactiveResourcesDeactivatesAssignmentsForEachInactiveResource() {
        Resource activeResource = Resource.builder()
                .id(1L)
                .status("ACTIVE")
                .build();
        Resource inactiveResource = Resource.builder()
                .id(2L)
                .status("INACTIVE")
                .build();
        when(resourceRepository.findAll()).thenReturn(List.of(activeResource, inactiveResource));

        int resourcesProcessed = resourceService.deactivateAssignmentsForInactiveResources();

        assertThat(resourcesProcessed).isEqualTo(1);
        verify(assignmentService).deactivateAssignmentsByResourceId(inactiveResource.getId());
        verify(deviceAssignmentService).deactivateAssignmentsByResourceId(inactiveResource.getId());
        verify(assignmentService, never()).deactivateAssignmentsByResourceId(activeResource.getId());
        verify(deviceAssignmentService, never()).deactivateAssignmentsByResourceId(activeResource.getId());
    }
}
