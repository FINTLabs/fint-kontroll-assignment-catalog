package no.fintlabs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import no.fint.antlr.EnableFintFilter;
import no.fintlabs.slack.SlackMessenger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@EnableFintFilter
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SlackMessenger slackMessenger = null;

        try {
            AnnotationConfigApplicationContext tempContext = new AnnotationConfigApplicationContext();

            ConfigurableEnvironment env = new StandardEnvironment();
            YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
            env.getPropertySources().addLast(loader.load("applicationYaml", new ClassPathResource("application.yaml")).get(0));
            tempContext.setEnvironment(env);

            tempContext.registerBean(RestTemplate.class);
            tempContext.scan("no.fintlabs.slack");
            tempContext.refresh();

            slackMessenger = tempContext.getBean(SlackMessenger.class);
            tempContext.close();

            SpringApplication.run(Application.class, args);
        } catch (Exception ex) {
            log.error("Application failed to start!", ex);

            if (slackMessenger != null) {
                slackMessenger.sendErrorMessage("⚠️ *Application failed to start!* \n" +
                                                "🔹 *Message:* `" + (ex.getMessage() != null ? ex.getMessage() : ex.getCause().getMessage()) + "`\n" +
                                                "🔹 *Time:* " + LocalDateTime.now() + "\n" +
                                                "🔹 *Stack Trace:*\n ```" + getStackTrace(ex) + "```");
            } else {
                log.error("SlackMessenger bean could not be loaded. Unable to send Slack notification.");
            }
        }
    }

    private static String getStackTrace(Exception ex) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : ex.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString().length() > 3000 ? sb.substring(0, 3000) + "... (truncated)" : sb.toString();
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
