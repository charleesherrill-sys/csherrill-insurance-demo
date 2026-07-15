package com.aegis.integration;

/**
 * Base for the mocked third-party integrations.
 *
 * <p>PERFORMANCE (INTENTIONAL — see REVIEW.md): every integration call blocks the
 * request thread with a simulated network round-trip via {@link #simulateLatency}.
 * There is no async, no batching, and no caching, so the adjudication path pays
 * the full latency of each downstream call in series.
 */
abstract class ExternalCallSupport {

    /** Simulated synchronous network latency (ms). */
    protected void simulateLatency(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
