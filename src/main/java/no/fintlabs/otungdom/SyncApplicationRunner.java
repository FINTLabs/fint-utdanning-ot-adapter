package no.fintlabs.otungdom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs a full OT ungdom sync from Vigo to FINT on startup.
 * <p>
 * Controlled by {@code fint.sync.enabled}:
 * <ul>
 *   <li><b>true</b> (default): runs automatically.</li>
 *   <li><b>false</b>: disabled — useful for tests or local runs.</li>
 * </ul>
 * Exits with code 0 on success or 1 on failure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "fint.sync.enabled", havingValue = "true", matchIfMissing = true)
public class SyncApplicationRunner implements ApplicationRunner {

    private final OtUngdomSync otUngdomSync;

    @Override
    public void run(ApplicationArguments args) {
        try {
            otUngdomSync.syncOtUngdomFromVigoToFint().block();
            log.info("OT ungdom full-sync completed successfully");
            System.exit(0);
        } catch (Throwable t) {
            log.error("OT ungdom full-sync failed", t);
            System.exit(1);
        }
    }
}
