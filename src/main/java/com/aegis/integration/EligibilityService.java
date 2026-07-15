package com.aegis.integration;

import org.springframework.stereotype.Service;

/** Mocked third-party benefits-eligibility check (e.g. clearinghouse). */
@Service
public class EligibilityService extends ExternalCallSupport {

    /**
     * Returns true if the member is eligible for the given diagnosis on the date
     * of service. Blocks the calling thread to simulate a slow downstream API.
     */
    public boolean checkEligibility(long memberUserId, String diagnosisCode) {
        simulateLatency(120);
        // Mock rule: everything eligible except a couple of excluded codes.
        if (diagnosisCode == null) {
            return true;
        }
        return !"Z99.9".equalsIgnoreCase(diagnosisCode);
    }
}
