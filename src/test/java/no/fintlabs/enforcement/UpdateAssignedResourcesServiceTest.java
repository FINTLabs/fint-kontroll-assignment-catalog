package no.fintlabs.enforcement;

import no.fintlabs.resource.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateAssignedResourcesServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private LicenseEnforcementService licenseEnforcementService;

    @InjectMocks
    private UpdateAssignedResourcesService updateAssignedResourcesService;

    @Test
    void updateAssignedResources_recalculatesEveryResourceInBatches() {
        when(resourceRepository.findIdsByIdGreaterThanOrderByIdAsc(eq(0L), any()))
                .thenReturn(List.of(1L, 2L));

        int updatedResources = updateAssignedResourcesService.updateAssignedResources();

        assertThat(updatedResources).isEqualTo(2);
        verify(licenseEnforcementService).recalculateAssignedResourcesForResource(1L);
        verify(licenseEnforcementService).recalculateAssignedResourcesForResource(2L);
    }
}
