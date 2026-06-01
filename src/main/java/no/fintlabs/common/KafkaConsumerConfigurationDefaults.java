package no.fintlabs.common;

import no.novari.kafka.consuming.ErrorHandlerConfiguration;
import no.novari.kafka.consuming.ErrorHandlerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumerConfigurationDefaults {

    private final ErrorHandlerFactory errorHandlerFactory;

    public KafkaConsumerConfigurationDefaults(ErrorHandlerFactory errorHandlerFactory) {
        this.errorHandlerFactory = errorHandlerFactory;
    }

    public <T> CommonErrorHandler defaultErrorHandler() {
        return errorHandlerFactory.createErrorHandler(
                ErrorHandlerConfiguration.<T>stepBuilder()
                        .noRetries()
                        .skipFailedRecords()
                        .build()
        );
    }
}
