package no.fintlabs.membership;

import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MembershipConsumerTest {
    @Mock
    private EntityConsumerFactoryService factoryService;

    @Mock
    private FintCache<String, Membership> membershipCache;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private MembershipService membershipService;

    @InjectMocks
    private MembershipConsumer membershipConsumer;

    @Test
    public void process_shouldHandleNewMembership() {
        Membership membership = new Membership();
        membership.setId("1");

        ConsumerRecord<String, Membership> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", membership);

        Membership convertedMembership = new Membership();
        convertedMembership.setId("1");

        when(membershipCache.getOptional("1")).thenReturn(Optional.empty());
        when(membershipRepository.findById("1")).thenReturn(Optional.empty());
        when(membershipRepository.saveAndFlush(convertedMembership)).thenReturn(convertedMembership);

        membershipConsumer.processMemberships(consumerRecord);

        verify(membershipCache).put("1", convertedMembership);
        verify(membershipCache).getNumberOfEntries();
        verify(membershipService).syncAssignmentsForMembership(convertedMembership);
    }

    @Test
    public void process_shouldUpdateCachedMembershipWhenDifferent_deactivate() {
        Membership cachedMembership = new Membership();
        cachedMembership.setId("1");
        cachedMembership.setMemberStatus("Active");

        Membership incomingMembership = new Membership();
        incomingMembership.setId("1");
        incomingMembership.setMemberStatus("InActive");

        Membership savedMembership = new Membership();
        savedMembership.setId("1");
        savedMembership.setMemberStatus("InActive");

        ConsumerRecord<String, Membership> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", incomingMembership);

        when(membershipCache.getOptional("1")).thenReturn(Optional.of(cachedMembership));
        when(membershipRepository.findById("1")).thenReturn(Optional.of(cachedMembership));
        when(membershipRepository.saveAndFlush(incomingMembership)).thenReturn(savedMembership);

        membershipConsumer.processMemberships(consumerRecord);

        verify(membershipRepository).saveAndFlush(incomingMembership);
        verify(membershipCache).put("1", savedMembership);
        verify(membershipService).deactivateAssignmentsForMembership(savedMembership);
    }

    @Test
    public void process_shouldUpdateCachedMembershipWhenDifferent_changedStatus() {
        Membership cachedMembership = new Membership();
        cachedMembership.setId("1");
        cachedMembership.setMemberStatus("Active");

        Membership incomingMembership = new Membership();
        incomingMembership.setId("1");
        incomingMembership.setMemberStatus("Whatever");

        Membership savedMembership = new Membership();
        savedMembership.setId("1");
        savedMembership.setMemberStatus("Whatever");

        ConsumerRecord<String, Membership> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", incomingMembership);

        when(membershipCache.getOptional("1")).thenReturn(Optional.of(cachedMembership));
        when(membershipRepository.findById("1")).thenReturn(Optional.of(cachedMembership));
        when(membershipRepository.saveAndFlush(incomingMembership)).thenReturn(savedMembership);

        membershipConsumer.processMemberships(consumerRecord);

        verify(membershipRepository).saveAndFlush(incomingMembership);
        verify(membershipCache).put("1", savedMembership);
        verify(membershipService).syncAssignmentsForMembership(savedMembership);
    }
}
