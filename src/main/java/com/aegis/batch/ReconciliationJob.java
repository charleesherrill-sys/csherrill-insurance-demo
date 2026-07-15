package com.aegis.batch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Schedules the nightly reconciliation run. */
@Component
public class ReconciliationJob {

    private final ReconciliationService reconciliationService;

    @Autowired
    public ReconciliationJob(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    /** Runs every night at 02:00 server time. */
    @Scheduled(cron = "0 0 2 * * *")
    public void nightly() {
        ReconciliationService.ReconciliationResult result = reconciliationService.run();
        System.out.println("[batch] reconciliation " + result.getStatus()
                + " matched=" + result.getMatched()
                + " unmatched=" + result.getUnmatched());
    }
}
