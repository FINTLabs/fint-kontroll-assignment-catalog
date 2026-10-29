package no.fintlabs.assignment.device.entra;

import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.EntraReturnCode;
import no.fintlabs.assignment.device.entra.DeviceEntraMembershipPublisher;
import no.fintlabs.entra.membership.MembershipStatus;
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
    private DeviceEntraMembershipPublisher deviceAssigmentEntityProducerService;

    @InjectMocks
    private EntraGroupDeviceMembershipConsumer consumer;

    private UUID deviceEntraId;
    private UUID resourceEntraId;
    private DeviceEntraMembership deviceEntraMembership;

    @BeforeEach
    void setUp() {
        deviceEntraId = UUID.randomUUID();
        resourceEntraId = UUID.randomUUID();
        deviceEntraMembership = DeviceEntraMembership.builder()
                .deviceEntraId(deviceEntraId)
                .resourceEntraId(resourceEntraId)
                .build();
    }

    @Test
    void processGroupMembership_shouldSetStatusConfirmed_whenCodeIsAddedAndAlreadyActive() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.ADDED);
        deviceEntraMembership.setMembershipStatus(MembershipStatus.ACTIVE);
        
        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceEntraId, resourceEntraId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED));
        verifyNoInteractions(deviceAssigmentEntityProducerService);
    }

    @Test
    void processGroupMembership_shouldPublish_whenCodeIsAddedAndNotActive() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.ADDED);
        deviceEntraMembership.setMembershipStatus(MembershipStatus.INACTIVE);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceEntraId, resourceEntraId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceAssigmentEntityProducerService).publish(deviceEntraMembership, true);
        verify(deviceEntraMembershipRepository).save(any());
    }

    @Test
    void processGroupMembership_shouldSetStatusDeletionConfirmed_whenCodeIsRemovedAndAlreadyInactive() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.REMOVED);
        deviceEntraMembership.setMembershipStatus(MembershipStatus.INACTIVE);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceEntraId, resourceEntraId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraMembershipSyncStatus.DELETION_CONFIRMED));
    }

    @Test
    void processGroupMembership_shouldSetStatusNeedsRepublish_whenCodeIsFailed() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.FAILED);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceEntraId, resourceEntraId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraMembershipSyncStatus.NEEDS_REPUBLISH));
    }

    @Test
    void processGroupMembership_shouldSetStatusError_whenCodeIsError() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.ERROR);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceEntraId, resourceEntraId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraMembershipSyncStatus.ERROR));
    }

    @Test
    void processGroupMembership_shouldSetDeletionConfirmed_whenCodeIsNoChangesAndInactive() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.NO_CHANGES);
        deviceEntraMembership.setMembershipStatus(MembershipStatus.INACTIVE);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceEntraId, resourceEntraId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraMembershipSyncStatus.DELETION_CONFIRMED));
        verifyNoInteractions(deviceAssigmentEntityProducerService);
    }

    private EntraDeviceGroupMembership createPayload(EntraReturnCode code) {
        return EntraDeviceGroupMembership.builder()
                .entraDeviceRef(deviceEntraId)
                .entraGroupRef(resourceEntraId)
                .code(code)
                .build();
    }
}
