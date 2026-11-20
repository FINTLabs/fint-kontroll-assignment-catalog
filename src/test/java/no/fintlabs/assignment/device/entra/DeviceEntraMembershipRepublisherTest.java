package no.fintlabs.assignment.device.entra;

import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import no.fintlabs.device.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceEntraMembershipRepublisherTest {

    @Mock
    private DeviceEntraMembershipRepository deviceEntraMembershipRepository;

    @Mock
    private DeviceEntraMembershipPublisher publisher;

    private DeviceEntraMembershipRepublisher deviceMembershipRepublisher;

    @BeforeEach
    void setUp() {
        deviceMembershipRepublisher = new DeviceEntraMembershipRepublisher(deviceEntraMembershipRepository, publisher, 72);
    }

    @Test
    void scheduledRepublishingUsesTheConfiguredResponseTimeoutAndOneDayFailureDelay() {
        long before = System.currentTimeMillis();

        deviceMembershipRepublisher.republishStaleMemberships();

        verify(deviceEntraMembershipRepository).findStaleMembershipsByIdGreaterThanOrderByIdAsc(
                anyList(), any(), any(), any(), any(), any(),
                argThat(cutoff -> isWithinOneSecondOf(cutoff, before - 72L * 60 * 60 * 1000)),
                argThat(cutoff -> isWithinOneSecondOf(cutoff, before - 24L * 60 * 60 * 1000)),
                eq(0L), any());
    }

    private boolean isWithinOneSecondOf(Date value, long expectedMillis) {
        return Math.abs(value.getTime() - expectedMillis) < 1_000;
    }

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
        verify(publisher).publish(membership, true);
    }

    @Test
    void republishStaleMemberships_shouldPublishOnlyMembershipsReturnedByTheStaleQuery() {
        DeviceEntraMembership membership = DeviceEntraMembership.builder()
                .id(1L)
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.SENT)
                .build();
        when(deviceEntraMembershipRepository.findStaleMembershipsByIdGreaterThanOrderByIdAsc(
                anyList(), eq(MembershipStatus.ACTIVE), any(), eq(MembershipStatus.INACTIVE), any(), any(), any(), any(), eq(0L), any()))
                .thenReturn(List.of(membership));

        deviceMembershipRepublisher.republishStaleMemberships();

        verify(deviceEntraMembershipRepository).findStaleMembershipsByIdGreaterThanOrderByIdAsc(
                eq(List.of(EntraMembershipSyncStatus.NOT_SENT, EntraMembershipSyncStatus.TO_BE_DELETED)),
                eq(MembershipStatus.ACTIVE),
                eq(EntraMembershipSyncStatus.SENT),
                eq(MembershipStatus.INACTIVE),
                eq(EntraMembershipSyncStatus.DELETION_SENT),
                eq(EntraMembershipSyncStatus.NEEDS_REPUBLISH),
                any(), any(), eq(0L), any());
        verify(publisher).publish(membership, false);
    }
}
