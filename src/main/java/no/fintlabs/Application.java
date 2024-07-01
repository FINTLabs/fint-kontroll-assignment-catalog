package no.fintlabs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import no.fint.antlr.EnableFintFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableFintFilter
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
@EnableAsync
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public OpenAPI openAPIDescription() {
        return new OpenAPI()
                .info(new Info()
                              .title("FINT Kontroll assignment catalog")
                              .version("1.0.0")
                              .description("API's for assignment of resources to users")
                              .termsOfService("http://swagger.io/terms/")
                              .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .components(new Components()
                                    .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                            .type(SecurityScheme.Type.HTTP)
                                            .scheme("bearer")
                                            .bearerFormat("JWT")
                                            .in(SecurityScheme.In.HEADER)
                                            .description("Auth key")
                                            .name("Authorization"))
                )
                .addSecurityItem(new SecurityRequirement()
                                         .addList("bearer-jwt"))
                ;
    }

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(1500);
        executor.setThreadNamePrefix("assigcat-");
        executor.initialize();
        return executor;
    }
}
