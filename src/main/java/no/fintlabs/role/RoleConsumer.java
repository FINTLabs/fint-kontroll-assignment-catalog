package no.fintlabs.role;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RoleConsumer {
    private final RoleRepository roleRepository;
    private final AssignmentService assignmentService;
    private final FintCache<Long, Role> roleCache;

    public RoleConsumer(RoleRepository roleRepository, AssignmentService assignmentService, FintCache<Long, Role> roleCache) {
        this.roleRepository = roleRepository;
        this.assignmentService = assignmentService;
        this.roleCache = roleCache;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, Role> roleConsumerConfig(
            EntityConsumerFactoryService entityConsumerFactoryService
    ){
        return entityConsumerFactoryService.createFactory(
                        Role.class,
                        this::process)
                .createContainer(EntityTopicNameParameters
                        .builder()
                        .resource("role-catalog-role")
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
        log.info("Processing role update with id: {}", incomingRole.getId());

        roleRepository.findById(incomingRole.getId())
                .ifPresentOrElse(
                        existingRole -> updateRole(incomingRole, existingRole),
                        () -> {
                            log.info("Role is new. Saving {}", incomingRole.getId());
                            roleRepository.saveAndFlush(incomingRole);
                        }
                );

        roleCache.put(incomingRole.getId(), incomingRole);
    }

    private void updateRole(Role incomingRole, Role existingRole) {
        if (!existingRole.equals(incomingRole)) {
            log.info("Role {} already exists but has changes, updating role with id: ", incomingRole.getId());

            if (incomingRole.getRoleStatus() != null && !incomingRole.getRoleStatus().equalsIgnoreCase(existingRole.getRoleStatus())) {
                assignmentService.deactivateAssignmentsByRole(incomingRole);
            }

            roleRepository.saveAndFlush(incomingRole);
        }
    }

}
