package no.fintlabs.resource;

import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ResourceAvailabilityPublishingComponentTest {

    private final ResourceAvailabilityProducerService producerService = mock(ResourceAvailabilityProducerService.class);
    private final ResourceAvailabilityPublishingComponent publishingComponent =
            new ResourceAvailabilityPublishingComponent(producerService);

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldPublishAvailabilityOnlyAfterTransactionCommit() {
        Resource resource = Resource.builder()
                .resourceId("resource-id")
                .numberOfResourcesAssigned(3L)
                .build();
        ApplicationResourceLocation location = ApplicationResourceLocation.builder()
                .orgUnitId("org-unit")
                .numberOfResourcesAssigned(2L)
                .build();
        TransactionSynchronizationManager.initSynchronization();

        publishingComponent.updateResourceAvailability(location, resource);

        verifyNoInteractions(producerService);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCommit());

        verify(producerService).publish(org.mockito.ArgumentMatchers.any(ResourceAvailabilityDTO.class));
    }
}
