package no.fintlabs.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.novari.kafka.producing.ProducerFactory;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BatchingKafkaProducerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BatchingKafkaProducerConfiguration.class, TestConfiguration.class)
            .withPropertyValues(
                    "spring.kafka.producer.batch-size=32768",
                    "spring.kafka.producer.buffer-memory=67108864",
                    "spring.kafka.producer.compression-type=lz4",
                    "spring.kafka.producer.properties.linger.ms=10"
            );

    @Test
    void novariProducerFactoryUsesSpringProducerSettings() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            var producerFactory = (DefaultKafkaProducerFactory<String, Object>) context
                    .getBean(ProducerFactory.class).createFactory(Object.class);
            Map<String, Object> properties = producerFactory.getConfigurationProperties();

            assertThat(properties).containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
                    .containsEntry(ProducerConfig.CLIENT_ID_CONFIG, "novari-producer")
                    .containsEntry(ProducerConfig.BATCH_SIZE_CONFIG, 32768)
                    .containsEntry(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864L)
                    .containsEntry(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4")
                    .containsEntry(ProducerConfig.LINGER_MS_CONFIG, "10");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(KafkaProperties.class)
    static class TestConfiguration {

        @Bean("producerConfig")
        ProducerConfig novariProducerConfig() {
            return new ProducerConfig(Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                    ProducerConfig.CLIENT_ID_CONFIG, "novari-producer",
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
            ));
        }

        @Bean
        ProducerFactory producerFactory(ProducerConfig producerConfig) {
            return new ProducerFactory(producerConfig, new ObjectMapper());
        }
    }
}
