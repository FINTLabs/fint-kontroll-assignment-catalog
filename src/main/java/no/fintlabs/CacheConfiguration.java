package no.fintlabs;

import no.fintlabs.applicationResourceLocation.ApplicationResourceLocation;
import no.fintlabs.cache.FintCache;
import no.fintlabs.cache.FintCacheManager;
import no.fintlabs.membership.Membership;
import no.fintlabs.role.Role;
import no.fintlabs.user.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
public class CacheConfiguration {

    private final FintCacheManager fintCacheManager;

    public CacheConfiguration(FintCacheManager fintCacheManager) {
        this.fintCacheManager = fintCacheManager;
    }

    @Bean
    FintCache<String, Membership> membershipCache() {
        return createCacheStringKey(Membership.class);
    }

    @Bean
    FintCache<Long, Role> roleCache() {
        return createCacheLongKey(Role.class);
    }

    @Bean
    FintCache<Long, User> userCache() {
        return createCacheLongKey(User.class);
    }

    private <V> FintCache<String, V> createCacheStringKey(Class<V> resourceClass) {
        return fintCacheManager.createCache(
                resourceClass.getName().toLowerCase(Locale.ROOT),
                String.class,
                resourceClass
        );
    }

    private <V> FintCache<Long, V> createCacheLongKey(Class<V> resourceClass) {
        return fintCacheManager.createCache(
                resourceClass.getName().toLowerCase(Locale.ROOT),
                Long.class,
                resourceClass
        );
    }
}
