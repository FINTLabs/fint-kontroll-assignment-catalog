package no.fintlabs.assignment.device.entra;

import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import no.fintlabs.device.Device;
import no.fintlabs.assignment.device.entra.DeviceEntraMembershipPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void republishStaleMemberships_shouldPublishOnlyMembershipsReturnedByTheStaleQuery() {
        DeviceEntraMembership membership = DeviceEntraMembership.builder()
                .id(1L)
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.SENT)
                .build();
        when(deviceEntraMembershipRepository.findStaleMembershipsByIdGreaterThanOrderByIdAsc(
                anyList(), eq(MembershipStatus.ACTIVE), anyList(), eq(MembershipStatus.INACTIVE), anyList(), any(), eq(0L), any()))
                .thenReturn(List.of(membership));

        deviceMembershipRepublisher.republishStaleMemberships();

        verify(deviceEntraMembershipRepository).findStaleMembershipsByIdGreaterThanOrderByIdAsc(
                eq(List.of(EntraMembershipSyncStatus.NOT_SENT, EntraMembershipSyncStatus.TO_BE_DELETED)),
                eq(MembershipStatus.ACTIVE),
                eq(List.of(EntraMembershipSyncStatus.SENT, EntraMembershipSyncStatus.NEEDS_REPUBLISH)),
                eq(MembershipStatus.INACTIVE),
                eq(List.of(EntraMembershipSyncStatus.DELETION_SENT, EntraMembershipSyncStatus.NEEDS_REPUBLISH)),
                any(), eq(0L), any());
        verify(deviceAssigmentEntityProducerService).publish(membership, false);
    }
}
