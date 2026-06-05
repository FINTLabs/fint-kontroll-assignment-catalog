package no.fintlabs.device.entra;

import no.fintlabs.entra.EntraStatus;
import no.fintlabs.device.DeviceAssigmentEntityProducerService;
import no.fintlabs.entra.MembershipStatus;
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
        EntraDeviceGroupMembership payload = createPayload(no.fintlabs.groupmembership.EntraStatus.ADDED);
        deviceEntraMembership.setMembershipStatus(MembershipStatus.ACTIVE);
        
        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceEntraId, resourceEntraId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraStatus.MEMBERSHIP_CONFIRMED));
        verifyNoInteractions(deviceAssigmentEntityProducerService);
    }

    @Test
    void processGroupMembership_shouldPublish_whenCodeIsAddedAndNotActive() {
        EntraDeviceGroupMembership payload = createPayload(no.fintlabs.groupmembership.EntraStatus.ADDED);
        deviceEntraMembership.setMembershipStatus(MembershipStatus.INACTIVE);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceEntraId, resourceEntraId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceAssigmentEntityProducerService).publish(deviceEntraMembership, true);
        verify(deviceEntraMembershipRepository).save(any());
    }

    @Test
    void processGroupMembership_shouldSetStatusDeletionConfirmed_whenCodeIsRemovedAndAlreadyInactive() {
        EntraDeviceGroupMembership payload = createPayload(no.fintlabs.groupmembership.EntraStatus.REMOVED);
        deviceEntraMembership.setMembershipStatus(MembershipStatus.INACTIVE);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceEntraId, resourceEntraId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraStatus.DELETION_CONFIRMED));
    }

    @Test
    void processGroupMembership_shouldSetStatusNeedsRepublish_whenCodeIsFailed() {
        EntraDeviceGroupMembership payload = createPayload(no.fintlabs.groupmembership.EntraStatus.FAILED);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceEntraId, resourceEntraId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraStatus.NEEDS_REPUBLISH));
    }

    @Test
    void processGroupMembership_shouldSetStatusError_whenCodeIsError() {
        EntraDeviceGroupMembership payload = createPayload(no.fintlabs.groupmembership.EntraStatus.ERROR);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceEntraId, resourceEntraId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraStatus.ERROR));
    }

    @Test
    void processGroupMembership_shouldSetDeletionConfirmed_whenCodeIsNoChangesAndInactive() {
        EntraDeviceGroupMembership payload = createPayload(no.fintlabs.groupmembership.EntraStatus.NO_CHANGES);
        deviceEntraMembership.setMembershipStatus(MembershipStatus.INACTIVE);

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(deviceEntraId, resourceEntraId))
                .thenReturn(Optional.of(deviceEntraMembership));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraMembershipRepository).save(argThat(info -> info.getEntraStatus() == EntraStatus.DELETION_CONFIRMED));
        verifyNoInteractions(deviceAssigmentEntityProducerService);
    }

    private EntraDeviceGroupMembership createPayload(no.fintlabs.groupmembership.EntraStatus code) {
        return EntraDeviceGroupMembership.builder()
                .entraDeviceRef(deviceEntraId)
                .entraGroupRef(resourceEntraId)
                .code(code)
                .build();
    }
}
