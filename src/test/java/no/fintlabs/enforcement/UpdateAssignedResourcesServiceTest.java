package no.fintlabs.enforcement;

import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateAssignedResourcesServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ResourceCountService resourceCountService;

    @InjectMocks
    private UpdateAssignedResourcesService updateAssignedResourcesService;

    @Test
    void updateAssignedResources_recalculatesEveryResourceAndFlushes() {
        Resource activeResource = Resource.builder().id(1L).status("ACTIVE").build();
        Resource inactiveResource = Resource.builder().id(2L).status("INACTIVE").build();

        when(resourceRepository.findAll()).thenReturn(List.of(activeResource, inactiveResource));

        int updatedResources = updateAssignedResourcesService.updateAssignedResources();

        assertThat(updatedResources).isEqualTo(2);
        verify(resourceCountService).updateNumberOfLicenses(activeResource);
        verify(resourceCountService).updateNumberOfLicenses(inactiveResource);
        verify(resourceRepository).flush();
    }
}
