package no.fintlabs.resource;

import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceConsumerConfigurationTest {

    private final ResourceRepository resourceRepository = mock(ResourceRepository.class);
    private final ResourceService resourceService = mock(ResourceService.class);
    private final KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults = mock(KafkaConsumerConfigurationDefaults.class);
    private final ResourceConsumerConfiguration resourceConsumerConfiguration =
            new ResourceConsumerConfiguration(resourceRepository, resourceService, kafkaConsumerConfigurationDefaults);

    @Test
    void processResourceShouldSkipIncomingInactiveResourceWhenUnchanged() {
        Resource resource = Resource.builder()
                .id(1L)
                .status("INACTIVE")
                .build();
        when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));

        resourceConsumerConfiguration.processResource(record(resource));

        verify(resourceService, never()).saveUpdatedResource(any(Resource.class));
        verify(resourceService, never()).save(any(Resource.class));
    }

    @Test
    void processResourceShouldSaveUpdatedIncomingResource() {
        Resource existingResource = Resource.builder()
                .id(1L)
                .status("ACTIVE")
                .resourceName("Old name")
                .build();
        Resource incomingResource = Resource.builder()
                .id(1L)
                .status("INACTIVE")
                .resourceName("New name")
                .build();
        when(resourceRepository.findById(incomingResource.getId())).thenReturn(Optional.of(existingResource));

        resourceConsumerConfiguration.processResource(record(incomingResource));

        verify(resourceService).saveUpdatedResource(incomingResource);
    }

    @Test
    void processResourceShouldSkipIncomingActiveResourceWhenUnchanged() {
        Resource resource = Resource.builder()
                .id(1L)
                .status("ACTIVE")
                .build();
        when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));

        resourceConsumerConfiguration.processResource(record(resource));

        verify(resourceService, never()).save(resource);
    }

    private ConsumerRecord<String, Resource> record(Resource resource) {
        return new ConsumerRecord<>("resource-group", 0, 0, resource.getId().toString(), resource);
    }
}
