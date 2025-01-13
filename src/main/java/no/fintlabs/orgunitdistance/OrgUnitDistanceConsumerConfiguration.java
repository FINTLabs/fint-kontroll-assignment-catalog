package no.fintlabs.orgunitdistance;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
@Slf4j
public class OrgUnitDistanceConsumerConfiguration {

    @Bean
    public ConcurrentMessageListenerContainer<String, OrgUnitDistance> orgUnitDistanceContainer(
            EntityConsumerFactoryService entityConsumerFactoryService,
            OrgUnitDistanceService orgUnitDistanceService
    ) {
        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("orgunitdistance")
                .build();

        return entityConsumerFactoryService.createFactory(
                OrgUnitDistance.class,
                (ConsumerRecord<String,OrgUnitDistance> consumerRecord) ->{
                    log.debug("Processing orgunitdistance with id: {}", consumerRecord.value().getId());
                    orgUnitDistanceService.save(consumerRecord.value());
                }
        ).createContainer(entityTopicNameParameters);
    }
}
