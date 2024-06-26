package no.fintlabs;

import no.fintlabs.cache.FintCache;
import no.fintlabs.cache.FintCacheManager;
import no.fintlabs.membership.Membership;
import no.fintlabs.role.Role;
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
    FintCache<Integer, Role> roleCache() {
        return createCacheIntKey(Role.class);
    }

    private <V> FintCache<String, V> createCacheStringKey(Class<V> resourceClass) {
        return fintCacheManager.createCache(
                resourceClass.getName().toLowerCase(Locale.ROOT),
                String.class,
                resourceClass
        );
    }

    private <V> FintCache<Integer, V> createCacheIntKey(Class<V> resourceClass) {
        return fintCacheManager.createCache(
                resourceClass.getName().toLowerCase(Locale.ROOT),
                Integer.class,
                resourceClass
        );
    }
}
