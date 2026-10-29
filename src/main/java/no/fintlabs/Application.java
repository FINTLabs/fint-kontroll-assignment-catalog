package no.fintlabs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.fint.antlr.EnableFintFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@EnableFintFilter
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = {"no.fintlabs", "no.novari.kafka"})
public class Application {
    public static void main(String[] args) {
        try {
            SpringApplication.run(Application.class, args);
        } catch (Exception ex) {
            log.error("Application failed to start!", ex);
        }
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
                .addTagsItem(new Tag()
                        .name("Developer endpoints")
                        .description("Operational and maintenance endpoints restricted by @OnlyDevelopers."))
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
                                         .addList("bearer-jwt"));
    }

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("assigcat-");
        executor.setKeepAliveSeconds(120);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // Prevent task loss
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(300);
        executor.initialize();
        return executor;
    }
}
