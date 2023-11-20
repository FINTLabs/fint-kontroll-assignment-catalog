package no.fintlabs;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

// Make sure we import the security config from fint-kontroll. This config sets up OPA authorization.
@Configuration
@Import(no.fintlabs.securityconfig.FintKontrollSecurityConfig.class)
public class FintKontrollConfiguration {

}