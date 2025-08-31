package Assignment.Recipe_Generator.config;

import Assignment.Recipe_Generator.service.SeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class StartupConfig {

    private final SeedService seedService;

    @Value("${seed.on-start:false}")
    private boolean seedOnStart;

    @Bean
    public CommandLineRunner seedDatabase() {
        return args -> {
            if (seedOnStart) {
                log.info("Seeding database on startup...");
                try {
                    seedService.seedDatabase();
                    log.info("Database seeding completed successfully");
                } catch (Exception e) {
                    log.error("Failed to seed database on startup", e);
                }
            } else {
                log.info("Database seeding skipped (seed.on-start=false)");
            }
        };
    }
}



