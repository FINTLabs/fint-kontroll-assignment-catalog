package no.fintlabs.applicationresourcelocation;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.KafkaEntityTopics;
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService;
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
            ParameterizedListenerContainerFactoryService entityConsumerFactoryService,
            ApplicationResourceLocationRepository applicationResourceLocationRepository,
            ApplicationResourceLocationService applicationResourceLocationService) {

        return entityConsumerFactoryService.createRecordListenerContainerFactory(
                        ApplicationResourceLocation.class,
                        (ConsumerRecord<String, ApplicationResourceLocation> consumerRecord) -> {

                            String key = consumerRecord.key();
                            ApplicationResourceLocation incoming = consumerRecord.value();

                            if (incoming == null) {
                                log.info("Received tombstone for key: {}", key);
                                applicationResourceLocationService.deleteById(Long.parseLong(key));
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
                        },
                        KafkaEntityTopics.defaultListenerConfiguration(),
                        null)
                .createContainer(KafkaEntityTopics.topicNameParameters("applicationresourcelocation-extended"));
    }



}
