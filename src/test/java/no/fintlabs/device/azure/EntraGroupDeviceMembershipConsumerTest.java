package no.fintlabs.device.azure;

import no.fintlabs.device.EntraStatus;
import no.fintlabs.device.DeviceAssigmentEntityProducerService;
import no.fintlabs.device.MembershipStatus;
import no.fintlabs.device.entra.EntraGroupDeviceMembershipConsumer;
import no.fintlabs.device.entra.EntraDeviceGroupMembership;
import no.fintlabs.device.entra.EntraReturnCode;
import no.fintlabs.device.entra.DeviceEntraMembership;
import no.fintlabs.device.entra.DeviceEntraMembershipRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntraGroupDeviceMembershipConsumerTest {

    @Mock
    private DeviceEntraMembershipRepository deviceEntraMembershipRepository;

    @Mock
    private DeviceAssigmentEntityProducerService deviceAssigmentEntityProducerService;

    @InjectMocks
    private EntraGroupDeviceMembershipConsumer consumer;

    private UUID deviceAzureId;
    private UUID resourceAzureId;
    private DeviceEntraMembership deviceEntraMembership;

    @BeforeEach
    void setUp() {
        deviceAzureId = UUID.randomUUID();
        resourceAzureId = UUID.randomUUID();
        deviceEntraMembership = DeviceEntraMembership.builder()
                .deviceEntraId(deviceAzureId)
                .resourceEntraId(resourceAzureId)
                .build();
    }

    @Test
    void processGroupMembership_shouldSetStatusConfirmed_whenCodeIsAddedAndAlreadyActive() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.ADDED);
        deviceEntraMembership.setMembershipStatus(MembershipStatus.ACTIVE);
        
        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraStatus.MEMBERSHIP_CONFIRMED));
        verifyNoInteractions(deviceAssigmentEntityProducerService);
    }

    @Test
    void processGroupMembership_shouldPublish_whenCodeIsAddedAndNotActive() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.ADDED);
        deviceEntraMembership.setMembershipStatus(MembershipStatus.INACTIVE);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceAssigmentEntityProducerService).publish(deviceEntraMembership, true);
        verify(deviceEntraMembershipRepository).save(any());
    }

    @Test
    void processGroupMembership_shouldSetStatusDeletionConfirmed_whenCodeIsRemovedAndAlreadyInactive() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.REMOVED);
        deviceEntraMembership.setMembershipStatus(MembershipStatus.INACTIVE);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraStatus.DELETION_CONFIRMED));
    }

    @Test
    void processGroupMembership_shouldSetStatusNeedsRepublish_whenCodeIsFailed() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.FAILED);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraStatus.NEEDS_REPUBLISH));
    }

    @Test
    void processGroupMembership_shouldSetStatusError_whenCodeIsError() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.ERROR);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraStatus.ERROR));
    }

    @Test
    void processGroupMembership_shouldSetDeletionConfirmed_whenCodeIsNoChangesAndInactive() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.NO_CHANGES);
        deviceEntraMembership.setMembershipStatus(MembershipStatus.INACTIVE);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraStatus.DELETION_CONFIRMED));
        verifyNoInteractions(deviceAssigmentEntityProducerService);
    }

    private EntraDeviceGroupMembership createPayload(EntraReturnCode code) {
        return EntraDeviceGroupMembership.builder()
                .entraDeviceRef(deviceAzureId)
                .entraGroupRef(resourceAzureId)
                .code(code)
                .build();
    }
}
