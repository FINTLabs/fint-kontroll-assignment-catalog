package no.fintlabs.common;

import org.junit.jupiter.api.Test;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryConfigTest {

    @Test
    void kafkaRetryTemplate_shouldTryTenTimesBeforeFailing() {
        RetryTemplate retryTemplate = new RetryConfig().kafkaRetryTemplate();
        retryTemplate.setBackOffPolicy(new NoBackOffPolicy());
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> retryTemplate.execute(context -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("dependency not ready");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(attempts).hasValue(RetryConfig.KAFKA_RETRY_MAX_ATTEMPTS);
    }
}
