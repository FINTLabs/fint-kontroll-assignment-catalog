package no.fintlabs.orgunitdistance;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import no.fintlabs.kafka.KafkaEntityTopics;
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
@Slf4j
public class OrgUnitDistanceConsumerConfiguration {

    @Bean
    public ConcurrentMessageListenerContainer<String, OrgUnitDistance> orgUnitDistanceContainer(
            ParameterizedListenerContainerFactoryService entityConsumerFactoryService,
            OrgUnitDistanceService orgUnitDistanceService,
            KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults
    ) {
        return entityConsumerFactoryService.createRecordListenerContainerFactory(
                OrgUnitDistance.class,
                (ConsumerRecord<String,OrgUnitDistance> consumerRecord) -> {
                    log.debug("Processing orgunitdistance with id: {}", consumerRecord.value().getId());
                    orgUnitDistanceService.save(consumerRecord.value());
                },
                KafkaEntityTopics.defaultListenerConfiguration(),
                kafkaConsumerConfigurationDefaults.defaultErrorHandler()
        ).createContainer(KafkaEntityTopics.topicNameParameters("orgunitdistance"));
    }
}
