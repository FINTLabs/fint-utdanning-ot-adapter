package no.fintlabs;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.otungdom.OtUngdomSync;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    ApplicationRunner runOnce(OtUngdomSync sync, ConfigurableApplicationContext ctx) {
        return args -> {
            int exitCode = 0;
            try {
                sync.syncOtUngdomFromVigoToFint().block();
                log.info("OT ungdom full-sync completed successfully, shutting down.");
            } catch (Throwable t) {
                exitCode = 1;
                log.error("OT ungdom full-sync failed - shutting down with non-zero exit.", t);
            }
            System.exit(exitCode);
        };
    }

}
