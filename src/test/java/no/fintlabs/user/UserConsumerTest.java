package no.fintlabs.user;

import no.fintlabs.cache.FintCache;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserConsumerTest {

    @Mock
    private FintCache<Long, User> userCache;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserConsumer userConsumer;

    @Test
    void process_shouldHandleNewUser() {
        KontrollUser kontrollUser = new KontrollUser();
        kontrollUser.setId(1L);
        ConsumerRecord<String, KontrollUser> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", kontrollUser);
        User convertedUser = new User();
        convertedUser.setId(1L);

        when(userCache.getOptional(1L)).thenReturn(Optional.empty());

        when(userService.saveUser(convertedUser)).thenReturn(convertedUser);

        userConsumer.process(consumerRecord);

        verify(userCache).put(1L, convertedUser);
        verify(userService).saveUser(convertedUser);
    }

    @Test
    void process_shouldUpdateCachedUserWhenDifferent() {
        KontrollUser kontrollUser = new KontrollUser();
        kontrollUser.setId(1L);
        kontrollUser.setStatus("Inactive");

        ConsumerRecord<String, KontrollUser> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", kontrollUser);

        User cachedUser = new User();
        cachedUser.setId(1L);
        cachedUser.setStatus("Active");

        User convertedUser = new User();
        convertedUser.setId(1L);
        convertedUser.setStatus("Inactive");

        User dbUser = new User();
        dbUser.setId(1L);
        dbUser.setStatus("Active");

        when(userCache.getOptional(1L)).thenReturn(Optional.of(cachedUser));
        when(userService.findById(1L)).thenReturn(Optional.of(dbUser));

        when(userService.updateUser(dbUser, convertedUser)).thenReturn(dbUser);

        userConsumer.process(consumerRecord);

        verify(userCache).put(1L, dbUser);
        verify(userService).updateUser(cachedUser, convertedUser);
    }

    @Test
    void process_shouldNotUpdateCachedUserWhenSame() {
        KontrollUser kontrollUser = new KontrollUser();
        kontrollUser.setId(1L);
        ConsumerRecord<String, KontrollUser> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", kontrollUser);

        User cachedUser = new User();
        cachedUser.setId(1L);

        User convertedUser = new User();
        convertedUser.setId(1L);

        when(userCache.getOptional(1L)).thenReturn(Optional.of(cachedUser));

        userConsumer.process(consumerRecord);

        verify(userCache, never()).put(anyLong(), any(User.class));
        verify(userService, never()).updateUser(any(User.class), any(User.class));
    }

    @Test
    void updateUserInCache_shouldSaveNewUserWhenNotFoundInService() {
        User user = new User();
        user.setId(1L);

        when(userService.findById(1L)).thenReturn(Optional.empty());
        when(userService.saveUser(user)).thenReturn(user);

        userConsumer.updateUserInCache(user, "log message");

        verify(userCache).put(1L, user);
        verify(userService).saveUser(user);
    }

    @Test
    void updateUserInCache_shouldUpdateExistingUserWhenFoundInService() {
        User user = new User();
        user.setId(1L);
        User existingUser = new User();
        existingUser.setId(1L);

        when(userService.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userService.updateUser(existingUser, user)).thenReturn(user);

        userConsumer.updateUserInCache(user, "log message");

        verify(userCache).put(1L, user);
        verify(userService).updateUser(existingUser, user);
    }
}
