package no.fintlabs.applicationresourcelocation;

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
public class ApplicationResourceLocationConsumerConfiguration {

    @Bean
    public ConcurrentMessageListenerContainer<String, ApplicationResourceLocation> applicationResourceLocationConsumer(
            EntityConsumerFactoryService entityConsumerFactoryService,
            ApplicationResourceLocationRepository applicationResourceLocationRepository
    ) {
        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("applicationresourcelocation-extended")
                .build();

        return entityConsumerFactoryService.createFactory(
                        ApplicationResourceLocation.class,
                        (ConsumerRecord<String, ApplicationResourceLocation> consumerRecord) -> {

                            String key = consumerRecord.key();
                            ApplicationResourceLocation incoming = consumerRecord.value();

                            if (incoming == null) {
                                log.info("Received tombstone for key: {}", key);
                                applicationResourceLocationRepository.deleteById(Long.parseLong(key));
                                log.info("Deleted ApplicationResourceLocation with id: {}", key);
                                return;
                            }

                            log.info("Processing ApplicationResourceLocation with id: {} - for applicationResource: {}",
                                    incoming.getId(), incoming.getResourceId());

                            Optional<ApplicationResourceLocation> existingOpt =
                                    applicationResourceLocationRepository.findById(incoming.getId());

                            if (existingOpt.isPresent()) {
                                if (!existingOpt.get().equals(incoming)) {
                                    applicationResourceLocationRepository.save(incoming);
                                    log.info("Updated ApplicationResourceLocation {}", incoming.getId());
                                } else {
                                    log.info("ApplicationResourceLocation {} unchanged. Skipping update", incoming.getId());
                                }
                            } else {
                                applicationResourceLocationRepository.save(incoming);
                                log.info("Created ApplicationResourceLocation {}", incoming.getId());
                            }
                        })
                .createContainer(entityTopicNameParameters);
    }



}
