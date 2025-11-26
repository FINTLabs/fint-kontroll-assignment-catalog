package no.fintlabs.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate kafkaRetryTemplate() {
        return RetryTemplate.builder()
                .exponentialBackoff(5000, 2.0, 100000)
                .maxAttempts(5)
                .build();
    }
}
