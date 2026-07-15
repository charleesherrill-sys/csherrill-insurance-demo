package com.aegis.claims.service;

import com.aegis.claims.model.Claim;
import com.aegis.claims.repository.ClaimRepository;
import com.aegis.integration.EligibilityService;
import com.aegis.policy.service.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Claim intake + validation: the "submit" and "validate" steps of the core flow
 * (submit -> validate -> adjudicate -> trigger payment).
 */
@Service
public class ClaimIntakeService {

    private final ClaimRepository claimRepository;
    private final PolicyService policyService;
    private final EligibilityService eligibilityService;

    @Autowired
    public ClaimIntakeService(ClaimRepository claimRepository,
                              PolicyService policyService,
                              EligibilityService eligibilityService) {
        this.claimRepository = claimRepository;
        this.policyService = policyService;
        this.eligibilityService = eligibilityService;
    }

    /** Submits a new claim in SUBMITTED status. Returns the new claim id. */
    public long submit(long memberUserId, long policyId, String claimType,
                       long amountCents, String diagnosisCode) {
        Claim claim = new Claim();
        claim.setMemberUserId(memberUserId);
        claim.setPolicyId(policyId);
        claim.setClaimType(claimType);
        claim.setAmountCents(amountCents);
        claim.setDiagnosisCode(diagnosisCode);
        return claimRepository.insert(claim);
    }

    /**
     * Validates a submitted claim: the policy must be active and the member must be
     * eligible for the diagnosis. Moves the claim to VALIDATED or DENIED.
     */
    public boolean validate(long claimId) {
        Claim claim = claimRepository.findById(claimId);
        if (claim == null) {
            throw new IllegalArgumentException("no such claim: " + claimId);
        }
        if (!policyService.isPolicyActive(claim.getPolicyId())) {
            claimRepository.updateStatus(claimId, "DENIED");
            return false;
        }
        boolean eligible = eligibilityService.checkEligibility(
                claim.getMemberUserId(), claim.getDiagnosisCode());
        if (!eligible) {
            claimRepository.updateStatus(claimId, "DENIED");
            return false;
        }
        claimRepository.updateStatus(claimId, "VALIDATED");
        return true;
    }
}
