package no.fintlabs.reporting;

import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.AssignmentPublishingComponent;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.authorization.AuthorizationUtil;
import no.fintlabs.opa.OpaApiClient;
import no.fintlabs.opa.OpaService;
import no.fintlabs.opa.RestTemplateOpaProvider;
import no.fintlabs.securityconfig.FintKontrollSecurityConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"testorg.testdomain.entity.flattened-assignment-reporting"})
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@ActiveProfiles("test")
public class FlattenedAssignmentReportProducerIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private FlattenedAssignmentRepository flattenedAssignmentRepository;

    @Autowired
    private FlattenedAssignmentReportService flattenedAssignmentReportService;

    @MockBean
    private AssigmentEntityProducerService assigmentEntityProducerService;

    @MockBean
    private OpaService opaService;

    @MockBean
    private AssignmentPublishingComponent assignmentPublishingComponent;

    @MockBean
    private AuthorizationUtil authorizationUtil;

    @MockBean
    private RestTemplateOpaProvider restTemplateOpaProvider;

    @MockBean
    private OpaApiClient opaApiClient;

    @MockBean
    private FintKontrollSecurityConfig fintKontrollSecurityConfig;

    private static final String topic = "testorg.testdomain.entity.flattened-assignment-reporting";

    private static final String applicationId = "fint-kontroll-assignment-catalog";
    private static final String topicOrgId = "testorg";
    private static final String topicDomainContext = "testdomain";

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("fint.kafka.topic.org-id", () -> topicOrgId);
        registry.add("fint.kafka.topic.domain-context", () -> topicDomainContext);
        registry.add("fint.kafka.application-id", () -> applicationId);
    }

    @Test
    public void shouldProduceAllFlattenedAssignmentReports() {
        // 1. Insert 1 million dummy records
//        insertDummyAssignments(100_000);

        // 2. Measure time for report generation
        long startReportTime = System.nanoTime();
        flattenedAssignmentReportService.generateStatisticsReport();
        long endReportTime = System.nanoTime();
        long reportDurationMillis = TimeUnit.NANOSECONDS.toMillis(endReportTime - startReportTime);
        System.out.println("Time taken to generate and publish report: " + reportDurationMillis + " ms");

        // 3. Measure time to consume Kafka messages
        long startConsumeTime = System.nanoTime();
        List<FlattenedAssignmentReport> receivedReports = consumeKafkaMessages(100_000);
        long endConsumeTime = System.nanoTime();
        long consumeDurationMillis = TimeUnit.NANOSECONDS.toMillis(endConsumeTime - startConsumeTime);
        System.out.println("Time taken to consume messages from Kafka: " + consumeDurationMillis + " ms");

        // 4. Assert
        assertThat(receivedReports.size()).isEqualTo(100_000);
    }

    private List<FlattenedAssignmentReport> consumeKafkaMessages(int expectedCount) {
        Map<String, Object> consumerProps = new HashMap<>();
//        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, FlattenedAssignmentReport> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(topic));

        List<FlattenedAssignmentReport> receivedReports = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        while (receivedReports.size() < expectedCount && (System.currentTimeMillis() - startTime) < TimeUnit.MINUTES.toMillis(10)) {
            ConsumerRecords<String, FlattenedAssignmentReport> records = consumer.poll(Duration.ofSeconds(5));
            for (ConsumerRecord<String, FlattenedAssignmentReport> record : records) {
                receivedReports.add(record.value());
            }
        }
        consumer.close();
        return receivedReports;
    }
}
