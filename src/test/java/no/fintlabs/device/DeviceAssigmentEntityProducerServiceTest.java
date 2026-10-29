package no.fintlabs.device;

import no.fintlabs.assignment.device.entra.DeviceEntraMembershipPublisher;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import no.fintlabs.assignment.device.entra.DeviceEntraMembership;
import no.fintlabs.assignment.device.entra.DeviceEntraMembershipRepository;
import no.novari.kafka.producing.ParameterizedProducerRecord;
import no.novari.kafka.producing.ParameterizedTemplate;
import no.novari.kafka.producing.ParameterizedTemplateFactory;
import no.novari.kafka.topic.EventTopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceAssigmentEntityProducerServiceTest {

    @Mock
    private ParameterizedTemplateFactory entityProducerFactory;

    @Mock
    private EventTopicService entityTopicService;

    @Mock
    private ParameterizedTemplate eventProducer;

    @Mock
    private DeviceEntraMembershipRepository deviceEntraMembershipRepository;

    private DeviceEntraMembershipPublisher service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(entityProducerFactory.createTemplate(any())).thenReturn(eventProducer);
        service = new DeviceEntraMembershipPublisher(
                entityProducerFactory,
                entityTopicService,
                deviceEntraMembershipRepository
        );
    }

    @Test
    void publish_shouldSkipErrorMembershipWithoutForce() {
        DeviceEntraMembership membership = membership(EntraMembershipSyncStatus.ERROR, MembershipStatus.ACTIVE);

        service.publish(membership, false);

        verify(eventProducer, never()).send(any(ParameterizedProducerRecord.class));
        verify(deviceEntraMembershipRepository, never()).save(any());
    }

    @Test
    void publish_shouldForcePublishErrorMembershipWhenRequested() {
        DeviceEntraMembership membership = membership(EntraMembershipSyncStatus.ERROR, MembershipStatus.ACTIVE);

        service.publish(membership, true);

        verify(eventProducer).send(any(ParameterizedProducerRecord.class));
        verify(deviceEntraMembershipRepository).save(membership);
    }

    @Test
    void publish_shouldSendRemoveWhenMembershipIsInactive() {
        DeviceEntraMembership membership = membership(EntraMembershipSyncStatus.SENT, MembershipStatus.INACTIVE);

        service.publish(membership, false);

        verify(eventProducer).send(any(ParameterizedProducerRecord.class));
        verify(deviceEntraMembershipRepository).save(membership);
    }

    private DeviceEntraMembership membership(EntraMembershipSyncStatus entraStatus, MembershipStatus membershipStatus) {
        return DeviceEntraMembership.builder()
                .id(1L)
                .deviceEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .entraStatus(entraStatus)
                .membershipStatus(membershipStatus)
                .build();
    }
}
