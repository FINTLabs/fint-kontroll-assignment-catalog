package no.fintlabs.role;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.cache.FintCache;
import no.fintlabs.enforcement.LicenseEnforcementService;
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
    private final LicenseEnforcementService licenseEnforcementService;

    public RoleConsumer(RoleRepository roleRepository, AssignmentService assignmentService, FintCache<Long, Role> roleCache, LicenseEnforcementService licenseEnforcementService) {
        this.roleRepository = roleRepository;
        this.assignmentService = assignmentService;
        this.roleCache = roleCache;
        this.licenseEnforcementService = licenseEnforcementService;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, Role> roleConsumerConfig(
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {
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
        if (!existingRole.equals(incomingRole) && isIncomingRoleNewer(incomingRole, existingRole)) {
            log.info("Role id: {} already exists but has changes, updating role. Existing: {}, Incoming: {}", incomingRole.getId(), existingRole, incomingRole);
            long numberOfMembers = existingRole.getNoOfMembers() != null ? existingRole.getNoOfMembers() : 0;
            if (incomingRole.getRoleStatus() != null && !incomingRole.getRoleStatus().equalsIgnoreCase(existingRole.getRoleStatus()) && incomingRole.getRoleStatus().equalsIgnoreCase("inactive")) {
                log.info("Removed assigned resources for Role id: {} updated : {}", incomingRole.getId(), licenseEnforcementService.removeAllAssignedResourcesForRole(incomingRole,
                        numberOfMembers) ? "Success" : "Failure");
                assignmentService.deactivateAssignmentsByRole(incomingRole);
            } else {
                log.info("Assigned resources for Role id: {} updated : {}", incomingRole.getId(), licenseEnforcementService.updateAssignedResourcesWhenChangesInRole(incomingRole,
                        numberOfMembers) ? "Success" : "Failure");
            }

            roleRepository.saveAndFlush(incomingRole);
        }
    }

    private boolean isIncomingRoleNewer(Role incomingRole, Role existingRole) {
        if(incomingRole.getRoleStatusChanged() == null  || existingRole.getRoleStatusChanged() == null)
            return true;
        return !incomingRole.getRoleStatusChanged().before(existingRole.getRoleStatusChanged());
    }

}
