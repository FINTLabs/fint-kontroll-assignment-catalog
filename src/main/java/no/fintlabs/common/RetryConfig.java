package no.fintlabs.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryConfig {

    public static final int KAFKA_RETRY_MAX_ATTEMPTS = 3;

    @Bean
    public RetryTemplate kafkaRetryTemplate() {
        return RetryTemplate.builder()
                .exponentialBackoff(1000, 2.0, 2000)
                .maxAttempts(KAFKA_RETRY_MAX_ATTEMPTS)
                .build();
    }
}
