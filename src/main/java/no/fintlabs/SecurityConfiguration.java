package no.fintlabs;


import no.vigoiks.resourceserver.security.FintJwtUserConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
public class SecurityConfiguration {

    //    @Value("${fint.integration.service.authorized-role:rolle}")
//    private String authorizedRole;
    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf().disable()
                .authorizeExchange((authorize) -> authorize
                        .pathMatchers("/**")
                        .permitAll()
                        //.hasRole(authorizedRole)
                        .anyExchange()
                        .authenticated())
                .oauth2ResourceServer((resourceServer) -> resourceServer
                        .jwt()
                        .jwtAuthenticationConverter(new FintJwtUserConverter()));
        return http.build();
    }
}