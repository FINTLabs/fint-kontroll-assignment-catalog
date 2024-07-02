package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserConsumer {

    private final FintCache<Long, User> userCache;

    public UserConsumer(FintCache<Long, User> userCache) {
        this.userCache = userCache;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, User> userConsumerConfiguration(
            UserService userService,
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {
        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("kontrolluser")
                .build();

        ConcurrentMessageListenerContainer container = entityConsumerFactoryService.createFactory(
                        KontrollUser.class,
                        (ConsumerRecord<String, KontrollUser> consumerRecord) -> {
                            KontrollUser kontrollUser = consumerRecord.value();
                            log.info("Processing user: {}", kontrollUser.getId());

                            User convertedUser = User.builder()
                                    .id(kontrollUser.getId())
                                    .userName(kontrollUser.getUserName())
                                    .identityProviderUserObjectId(kontrollUser.getIdentityProviderUserObjectId())
                                    .firstName(kontrollUser.getFirstName())
                                    .lastName(kontrollUser.getLastName())
                                    .userType(kontrollUser.getUserType())
                                    .organisationUnitId(kontrollUser.getMainOrganisationUnitId())
                                    .organisationUnitName(kontrollUser.getMainOrganisationUnitName())
                                    .build();

                            userCache.getOptional(convertedUser.getId())
                                    .ifPresentOrElse(
                                            cachedUser -> {
                                                if (!cachedUser.equals(convertedUser)) {
                                                    log.info("User found in cache, but not equal, updating user: {}", convertedUser.getId());
                                                    User savedUser = userService.convertAndSaveAsUser(convertedUser);
                                                    userCache.put(savedUser.getId(), savedUser);
                                                }
                                            }
                                            , () -> {
                                                log.info("User not found in cache, saving user: {}", convertedUser.getId());
                                                User savedUser = userService.convertAndSaveAsUser(convertedUser);
                                                userCache.put(savedUser.getId(), savedUser);
                                            });
                        }
                )
                .createContainer(entityTopicNameParameters);

        return container;
    }
}
