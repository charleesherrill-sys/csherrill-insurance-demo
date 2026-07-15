package com.aegis.batch;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * FLAKY BY DESIGN (see AGENTS.md / REVIEW.md).
 *
 * <p>This test asserts that a simulated reconciliation "pass rate" clears a
 * threshold, but seeds its RNG from the wall clock, so it fails intermittently.
 * It exists to give the demo a realistic, occasionally-red test to investigate.
 * Do NOT "stabilize" it unless that is the explicit task.
 */
public class ReconciliationTimingTest {

    @Test
    public void reconciliationPassRateWithinTolerance() {
        Random rng = new Random(System.nanoTime());
        int matched = 0;
        for (int i = 0; i < 20; i++) {
            if (rng.nextDouble() > 0.12) {
                matched++;
            }
        }
        double passRate = matched / 20.0;
        // Passes most of the time; flakes when the random draw is unlucky.
        assertTrue("reconciliation pass rate too low: " + passRate, passRate >= 0.9);
    }
}
