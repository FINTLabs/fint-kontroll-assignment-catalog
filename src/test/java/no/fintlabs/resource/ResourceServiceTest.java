package no.fintlabs.resource;

import no.fintlabs.assignment.AssignmentService;
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
    private final ResourceService resourceService = new ResourceService(resourceRepository, assignmentService);

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
        verify(assignmentService, never()).deactivateAssignmentsByResourceId(activeResource.getId());
    }
}
