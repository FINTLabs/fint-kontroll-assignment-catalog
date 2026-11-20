package no.fintlabs.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class BatchingKafkaProducerConfiguration {

    @Bean
    @Primary
    ProducerConfig batchingProducerConfig(
            @Qualifier("producerConfig") ProducerConfig novariProducerConfig,
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> properties = new HashMap<>(novariProducerConfig.originals());
        properties.putAll(kafkaProperties.getProducer().buildProperties(null));
        return new ProducerConfig(properties);
    }
}
