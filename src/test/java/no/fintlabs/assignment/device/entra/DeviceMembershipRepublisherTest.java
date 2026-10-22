package no.fintlabs.assignment.device.entra;

import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.device.Device;
import no.fintlabs.assignment.device.entra.DeviceEntraMembershipPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceMembershipRepublisherTest {

    @Mock
    private DeviceEntraMembershipRepository deviceEntraMembershipRepository;

    @Mock
    private DeviceEntraMembershipPublisher deviceAssigmentEntityProducerService;

    @InjectMocks
    private DeviceMembershipRepublisher deviceMembershipRepublisher;

    @Test
    void republishErrorAssignmentsForDevice_shouldLookupByDataObjectId() {
        UUID dataObjectId = UUID.randomUUID();
        Device device = Device.builder()
                .id(1L)
                .sourceId("not-a-uuid-source-id")
                .dataObjectId(dataObjectId)
                .build();
        DeviceEntraMembership membership = DeviceEntraMembership.builder().build();

        when(deviceEntraMembershipRepository.findAllByEntraStatusAndDeviceEntraId(EntraMembershipSyncStatus.ERROR, dataObjectId))
                .thenReturn(List.of(membership));

        deviceMembershipRepublisher.republishErrorAssignmentsForDevice(device);

        verify(deviceEntraMembershipRepository).findAllByEntraStatusAndDeviceEntraId(EntraMembershipSyncStatus.ERROR, dataObjectId);
        verify(deviceAssigmentEntityProducerService).publish(membership, true);
    }
}
