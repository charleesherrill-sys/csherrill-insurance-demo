package com.aegis.integration;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Mock eligibility rules. */
public class EligibilityServiceTest {

    private final EligibilityService service = new EligibilityService();

    @Test
    public void excludedDiagnosisIsIneligible() {
        assertFalse(service.checkEligibility(5583, "Z99.9"));
    }

    @Test
    public void ordinaryDiagnosisIsEligible() {
        assertTrue(service.checkEligibility(5583, "J20.9"));
    }

    @Test
    public void nullDiagnosisIsEligible() {
        assertTrue(service.checkEligibility(5583, null));
    }
}
