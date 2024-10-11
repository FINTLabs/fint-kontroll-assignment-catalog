package no.fintlabs.role;

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

@Slf4j
@Component
public class RoleConsumer {
    private final RoleRepository roleRepository;
    private final FintCache<Long, Role> roleCache;

    public RoleConsumer(RoleRepository roleRepository, FintCache<Long, Role> roleCache) {
        this.roleRepository = roleRepository;
        this.roleCache = roleCache;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, Role> roleConsumerConfig(
            ParameterizedListenerContainerFactoryService parameterizedListenerContainerFactoryService
    ){
        return parameterizedListenerContainerFactoryService.createRecordListenerContainerFactory(
                        Role.class,
                        this::process,
                        ListenerConfiguration.builder()
                                .seekingOffsetResetOnAssignment(false)
                                .maxPollRecords(100)
                                .build())
                .createContainer(EntityTopicNameParameters
                        .builder()
                        .resourceName("role-catalog-role")
                        .topicNamePrefixParameters(TopicNamePrefixParameters.builder()
                                .orgIdApplicationDefault()
                                .domainContextApplicationDefault()
                                .build())
                        .build());
    }

    void process(ConsumerRecord<String, Role> consumerRecord) {
        Role incomingRole = consumerRecord.value();

        roleCache.getOptional(incomingRole.getId())
                .ifPresentOrElse(
                        cachedRole -> {
                            if (!cachedRole.equals(incomingRole)) {
                                processRoleUpdate(incomingRole);
                            }
                        }
                        , () -> processRoleUpdate(incomingRole));
    }

    void processRoleUpdate(Role incomingRole) {
        log.info("Processing role update: {}", incomingRole.getId());

        roleRepository.findById(incomingRole.getId())
                .ifPresentOrElse(
                        existingRole -> {
                            if (!existingRole.equals(incomingRole)) {
                                log.info("Role {} already exists but has changes, updating", incomingRole.getId());
                                roleRepository.save(incomingRole);
                            }
                        },
                        () -> {
                            log.info("Role is new. Saving {}", incomingRole.getId());
                            roleRepository.save(incomingRole);
                        }
                );

        roleCache.put(incomingRole.getId(), incomingRole);
    }
}
