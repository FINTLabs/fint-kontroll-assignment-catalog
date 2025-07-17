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
    private final ApplicationResourceLocationRepository applicationResourceLocationRepository;

    public ApplicationResourceLocationConsumerConfiguration(ApplicationResourceLocationRepository applicationResourceLocationRepository) {
        this.applicationResourceLocationRepository = applicationResourceLocationRepository;
    }


    @Bean
    public ConcurrentMessageListenerContainer<String, ApplicationResourceLocation> applicationResourceLocationConsumer(
            EntityConsumerFactoryService entityConsumerFactoryService,
            ApplicationResourceLocationService applicationResourceLocationService
    ) {
        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("applicationresourcelocation-extended")
                .build();

        return entityConsumerFactoryService.createFactory(
                        ApplicationResourceLocation.class,
                        (ConsumerRecord<String, ApplicationResourceLocation> consumerRecord) -> {
                            ApplicationResourceLocation incomingApplicationResourceLocation = consumerRecord.value();
                            log.info("Processing applicationResourceLocation with id: {} - for applicationResource: {}",
                                      consumerRecord.value().id, consumerRecord.value().resourceId);
                            Optional<ApplicationResourceLocation> applicationResourceLocationOptional =
                                    applicationResourceLocationRepository.findById(consumerRecord.value().id);
                            if (applicationResourceLocationOptional.isPresent()) {
                                ApplicationResourceLocation existingApplicationResourceLocation = applicationResourceLocationOptional.get();
                                if (!existingApplicationResourceLocation.equals(incomingApplicationResourceLocation)) {
                                    applicationResourceLocationRepository.save(incomingApplicationResourceLocation);
                                } else {
                                    log.info("ApplicationResourceLocation {} already exists and is equal to the incoming resource. Skipping update",
                                            incomingApplicationResourceLocation.getId());
                                }
                            } else {
                                applicationResourceLocationRepository.save(incomingApplicationResourceLocation);
                                log.info("ApplicationResourceLocation {} was created", incomingApplicationResourceLocation.getId());

                            }
                        })
                .createContainer(entityTopicNameParameters);
    }


}
