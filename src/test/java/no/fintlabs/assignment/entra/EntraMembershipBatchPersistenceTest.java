package no.fintlabs.assignment.entra;

import no.fintlabs.assignment.device.entra.DeviceEntraMembership;
import no.fintlabs.assignment.device.entra.DeviceEntraMembershipPublisher;
import no.fintlabs.assignment.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.assignment.device.entra.EntraDeviceGroupMembership;
import no.fintlabs.assignment.device.entra.EntraGroupDeviceMembershipProcessor;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.EntraReturnCode;
import no.fintlabs.entra.membership.MembershipStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({EntraGroupDeviceMembershipProcessor.class, EntraMembershipBatchLookup.class})
class EntraMembershipBatchPersistenceTest {

    @Autowired private DeviceEntraMembershipRepository repository;
    @Autowired private EntraGroupDeviceMembershipProcessor processor;
    @MockBean private DeviceEntraMembershipPublisher publisher;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void savesDetachedMembershipsTogether() {
        DeviceEntraMembership first = repository.save(membership());
        DeviceEntraMembership second = repository.save(membership());

        processor.processBatch(List.of(record(0, first), record(1, second)));

        assertThat(repository.findById(first.getId()).orElseThrow().getEntraStatus())
                .isEqualTo(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED);
        assertThat(repository.findById(second.getId()).orElseThrow().getEntraStatus())
                .isEqualTo(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED);
    }

    private DeviceEntraMembership membership() {
        return DeviceEntraMembership.builder()
                .deviceEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.SENT)
                .build();
    }

    private ConsumerRecord<String, EntraDeviceGroupMembership> record(long offset, DeviceEntraMembership membership) {
        EntraDeviceGroupMembership response = EntraDeviceGroupMembership.builder()
                .entraDeviceRef(membership.getDeviceEntraId())
                .entraGroupRef(membership.getResourceEntraId())
                .code(EntraReturnCode.ADDED)
                .build();
        return new ConsumerRecord<>("topic", 0, offset, "key-" + offset, response);
    }
}
