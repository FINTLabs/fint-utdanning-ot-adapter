package no.fintlabs.otungdom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Runs the cron job for ot-adapter the old way until we can enable FlaisJob.
 * This class should be deleted when FlaisJob is enabled.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CronRunner {
    private final OtUngdomSync otUngdomSync;

    @Scheduled(cron = "${fint.adapter.sync.cron}")
    public void run() {
        otUngdomSync.syncOtUngdomFromVigoToFint().block();
    }
}
