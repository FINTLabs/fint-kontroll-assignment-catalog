package no.fintlabs.role;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.Optional;

@Slf4j
@Configuration
public class RoleConsumerConfiguration {
    private final RoleRepository roleRepository;

    public RoleConsumerConfiguration(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, Role> roleConsumer(
            EntityConsumerFactoryService entityConsumerFactoryService
    ){
        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("role-catalog-role")
                .build();

        ConcurrentMessageListenerContainer container = entityConsumerFactoryService.createFactory(
                        Role.class,
                        (ConsumerRecord<String,Role> consumerRecord) -> {
                            Role incomingRole = consumerRecord.value();
                            log.info("Processing role: {}", incomingRole.getId());

                            Optional<Role> existingRoleOptional = roleRepository.findById(incomingRole.getId());

                            if (existingRoleOptional.isPresent()) {
                                Role existingRole = existingRoleOptional.get();
                                if (!existingRole.equals(incomingRole)) {
                                    roleRepository.save(incomingRole);
                                } else {
                                    log.info("Role {} already exists and is equal to the incoming role. Skipping.", incomingRole.getId());
                                }
                            } else {
                                roleRepository.save(incomingRole);
                            }
                        })
                .createContainer(entityTopicNameParameters);

        return container;
    }
}
