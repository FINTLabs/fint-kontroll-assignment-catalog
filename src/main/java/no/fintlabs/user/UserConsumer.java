package no.fintlabs.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserConsumer {

    private final FintCache<Long, User> userCache;

    private final UserService userService;

    @Bean
    public ConcurrentMessageListenerContainer<String, KontrollUser> userConsumerConfiguration(
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {
        EntityTopicNameParameters kontrolluser = EntityTopicNameParameters
                .builder()
                .resource("kontrolluser")
                .build();

        return entityConsumerFactoryService
                .createFactory(KontrollUser.class, this::process)
                .createContainer(kontrolluser);
    }

    void process(ConsumerRecord<String, KontrollUser> consumerRecord) {
        KontrollUser kontrollUser = consumerRecord.value();
        if (kontrollUser == null) {
            log.info("Skipping tombstone for key={}", consumerRecord.key());
            return;
        }

        log.info("Processing user: {}", kontrollUser.getId());
        User convertedUser = UserMapper.fromKontrollUser(kontrollUser);

        if ("DELETED".equalsIgnoreCase(convertedUser.getStatus())) {
            handleDeletedUser(convertedUser);
            return;
        }

        userCache.getOptional(convertedUser.getId())
                .ifPresentOrElse(
                        cachedUser -> handleCachedUser(cachedUser, convertedUser),
                        () -> handleNewUser(convertedUser)
                );
    }

    private void handleDeletedUser(User user) {
        log.info("User status=DELETED. Deactivating assignments and deleting user: {}", user.getId());

        userService.deactivateAssignmentsAndDeleteUser(user.getId());

        userCache.remove(user.getId());
    }
    private void handleCachedUser(User cachedUser, User convertedUser) {
        if (!cachedUser.convertedUserEquals(convertedUser)) {
            updateUserInCache(convertedUser, "User found in cache, but not equal, updating user: {}");
        } else {
            log.info("User in cache is up-to-date: {}", cachedUser.getId());
        }
    }

    private void handleNewUser(User user) {
        updateUserInCache(user, "User not found in cache, saving user: {}");
    }

    void updateUserInCache(User user, String logMessage) {
        log.info(logMessage, user.getId());

        userService.findById(user.getId())
                .ifPresentOrElse(
                        existingUser -> updateUser(existingUser, user),
                        () -> saveNewUser(user)
                );
    }

    private void updateUser(User existing, User updatedUser) {
        User saveduser = userService.updateUser(existing, updatedUser);
        userCache.put(saveduser.getId(), saveduser);
    }

    private void saveNewUser(User user) {
        User savedUser = userService.saveUser(user);
        userCache.put(savedUser.getId(), savedUser);
    }
}
