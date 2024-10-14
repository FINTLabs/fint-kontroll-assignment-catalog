package no.fintlabs.membership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.consuming.ListenerConfiguration;
import no.fintlabs.kafka.consuming.ParameterizedListenerContainerFactoryService;
import no.fintlabs.kafka.topic.name.EntityTopicNameParameters;
import no.fintlabs.kafka.topic.name.TopicNamePrefixParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MembershipConsumer {

    private final MembershipRepository membershipRepository;

    private final MembershipService membershipService;

    private final FintCache<String, Membership> membershipCache;

    public MembershipConsumer(MembershipRepository membershipRepository, MembershipService membershipService, FintCache<String, Membership> membershipCache) {
        this.membershipRepository = membershipRepository;
        this.membershipCache = membershipCache;
        this.membershipService = membershipService;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, Membership> membershipConsumerConfiguration(
            ParameterizedListenerContainerFactoryService parameterizedListenerContainerFactoryService
    ) {
        TopicNamePrefixParameters topicNamePrefixParameters = TopicNamePrefixParameters.builder()
                .orgIdApplicationDefault()
                .domainContextApplicationDefault()
                .build();

        return parameterizedListenerContainerFactoryService.createBatchListenerContainerFactory(
                        Membership.class,
                        this::processMemberships, ListenerConfiguration.builder()
                                .seekingOffsetResetOnAssignment(false)
                                .maxPollRecords(100)
                                .build())
                .createContainer(EntityTopicNameParameters.builder()
                                         .resourceName("role-catalog-membership")
                                         .topicNamePrefixParameters(topicNamePrefixParameters)
                                         .build());
    }

    private void processMemberships(List<ConsumerRecord<String, Membership>> consumerRecord) {
        List<Membership> toProcess = new ArrayList<>();

        consumerRecord.forEach(record -> {
            Membership incomingMembership = record.value();
            membershipCache.getOptional(incomingMembership.getId())
                    .ifPresentOrElse(
                            cachedMembership -> {
                                if (!cachedMembership.equals(incomingMembership)) {
                                    toProcess.add(incomingMembership);
                                }
                            }
                            , () -> toProcess.add(incomingMembership));
        });

        process(toProcess);
    }

    private void process(List<Membership> incomingMemberships) {
        List<String> membershipIds = incomingMemberships.stream().map(Membership::getId).toList();
        List<Membership> membershipsTopProcess = membershipRepository.findAllById(membershipIds);

        List<Membership> toSave = new ArrayList<>();

        incomingMemberships.forEach(incoming -> {
            Membership existing = membershipsTopProcess.stream().filter(m -> m.getId().equals(incoming.getId())).findFirst().orElse(null);
            if (existing != null && !existing.equals(incoming)) {
                log.info("Members are not equal. Saving: roleId {}, memberId {}, id {}", incoming.getRoleId(), incoming.getMemberId(), incoming.getId());
                toSave.add(incoming);
            }

            membershipCache.put(incoming.getId(), incoming);
        });

        List<Membership> savedMemberships = membershipRepository.saveAll(toSave);

        log.info("Membership cache size is now {}", membershipCache.getAll().size());

        savedMemberships.forEach(membershipService::processAssignmentsForMembership);
    }
}
