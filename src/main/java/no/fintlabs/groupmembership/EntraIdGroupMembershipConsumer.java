package no.fintlabs.groupmembership;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.entra.UserEntraMembership;
import no.fintlabs.assignment.entra.UserEntraMembershipRepository;
import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import no.fintlabs.entra.EntraStatus;
import no.fintlabs.entra.MembershipStatus;
import no.fintlabs.kafka.KafkaEventTopics;
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EntraIdGroupMembershipConsumer {

    private final UserEntraMembershipRepository userEntraMembershipRepository;
    private final AssigmentEntityProducerService assigmentEntityProducerService;
    private final KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults;

    @Bean
    public ConcurrentMessageListenerContainer<String, EntraIdGroupMembership> entraIdMembershipConsumer(
            ParameterizedListenerContainerFactoryService entityConsumerFactoryService
    ) {

        return entityConsumerFactoryService.createRecordListenerContainerFactory(
                        EntraIdGroupMembership.class,
                        this::processGroupMembership,
                        KafkaEventTopics.defaultListenerConfiguration(),
                        kafkaConsumerConfigurationDefaults.defaultErrorHandler())
                .createContainer(KafkaEventTopics.topicNameParameters(MembershipEventNames.ENTRA_USER_GROUP_MEMBERSHIP));

    }

    @Transactional
    public void processGroupMembership(ConsumerRecord<String, EntraIdGroupMembership> record) {
        EntraIdGroupMembership entraResponse = record.value();
        String key = record.key();

        if (entraResponse == null) {
            log.warn("Skipping null Entra membership result for key {}", key);
            return;
        }

        if (entraResponse.getCode() == null) {
            log.warn("Skipping Entra membership result with missing code for key {}", key);
            return;
        }

        UUID entraUserRef = entraResponse.getEntraUserRef();
        UUID entraGroupRef = entraResponse.getEntraGroupRef();
        Optional<UserEntraMembership> userEntraMembershipOptional =
                userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(entraUserRef, entraGroupRef);

        if (userEntraMembershipOptional.isEmpty()) {
            log.warn("No user Entra membership found for user {} and resource {}, messageKey: {}", entraUserRef, entraGroupRef, key);
            return;
        }

        UserEntraMembership userEntraMembership = userEntraMembershipOptional.get();
        log.info("Received response for user {} in group {}, messageKey: {}", entraUserRef, entraGroupRef, key);
        switch (entraResponse.getCode()) {
            case ADDED -> confirmMembershipAdded(userEntraMembership, key);
            case REMOVED -> confirmMembershipRemoved(userEntraMembership, key);
            case ERROR -> markAsError(userEntraMembership, key);
            case FAILED -> markAsFailed(userEntraMembership, key);
            case NO_CHANGES -> handleNoChangesResponse(userEntraMembership, key);
        }
        userEntraMembershipRepository.save(userEntraMembership);
    }

    private void handleNoChangesResponse(UserEntraMembership userEntraMembership, String key) {
        log.info("Received no changes response for user {} in group {}, messageKey: {}",
                userEntraMembership.getUserEntraId(),
                userEntraMembership.getResourceEntraId(),
                key);
        if (userEntraMembership.getMembershipStatus() == MembershipStatus.ACTIVE) {
            userEntraMembership.setEntraStatus(EntraStatus.MEMBERSHIP_CONFIRMED);
        } else if (userEntraMembership.getMembershipStatus() == MembershipStatus.INACTIVE) {
            userEntraMembership.setEntraStatus(EntraStatus.DELETION_CONFIRMED);
        }
    }

    private void markAsFailed(UserEntraMembership userEntraMembership, String key) {
        log.warn("Received failed status for user {} in group {}, messageKey: {}",
                userEntraMembership.getUserEntraId(),
                userEntraMembership.getResourceEntraId(),
                key);
        userEntraMembership.setEntraStatus(EntraStatus.NEEDS_REPUBLISH);
    }

    private void markAsError(UserEntraMembership userEntraMembership, String key) {
        log.warn("Received error for user {} in group {}, messageKey: {}",
                userEntraMembership.getUserEntraId(),
                userEntraMembership.getResourceEntraId(),
                key);
        userEntraMembership.setEntraStatus(EntraStatus.ERROR);
    }

    private void confirmMembershipRemoved(UserEntraMembership userEntraMembership, String key) {
        if (userEntraMembership.getMembershipStatus() != MembershipStatus.INACTIVE) {
            log.info("Received confirmation for removal of user {} from group {}, but local membership is active. Republishing addition, messageKey: {}",
                    userEntraMembership.getUserEntraId(),
                    userEntraMembership.getResourceEntraId(),
                    key);
            assigmentEntityProducerService.publish(userEntraMembership, true);
        } else {
            userEntraMembership.setEntraStatus(EntraStatus.DELETION_CONFIRMED);
        }
    }

    private void confirmMembershipAdded(UserEntraMembership userEntraMembership, String key) {
        if (userEntraMembership.getMembershipStatus() != MembershipStatus.ACTIVE) {
            log.info("Received confirmation for addition of user {} to group {}, but local membership is inactive. Republishing removal, messageKey: {}",
                    userEntraMembership.getUserEntraId(),
                    userEntraMembership.getResourceEntraId(),
                    key);
            assigmentEntityProducerService.publish(userEntraMembership, true);
        } else {
            userEntraMembership.setEntraStatus(EntraStatus.MEMBERSHIP_CONFIRMED);
        }
    }
}
