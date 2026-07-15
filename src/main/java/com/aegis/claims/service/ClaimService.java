package com.aegis.claims.service;

import com.aegis.claims.model.Claim;
import com.aegis.claims.repository.ClaimRepository;
import com.aegis.policy.model.Policy;
import com.aegis.policy.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read/query orchestration for claims.
 *
 * <p>PERFORMANCE (INTENTIONAL — see REVIEW.md): {@link #getClaimsForMember(long)}
 * exhibits a classic N+1 query pattern. It runs one query for the claim list and
 * then, for every claim, two more queries (service lines + policy). On a member
 * with many claims this is the flagship "losing money" latency issue on the
 * claims-list page. Do NOT batch/join this unless that is the explicit task.
 */
@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;

    @Autowired
    public ClaimService(ClaimRepository claimRepository, PolicyRepository policyRepository) {
        this.claimRepository = claimRepository;
        this.policyRepository = policyRepository;
    }

    public List<Claim> getClaimsForMember(long memberUserId) {
        List<Claim> claims = claimRepository.findByMember(memberUserId);
        // N+1: two extra round-trips per claim.
        for (Claim claim : claims) {
            claim.setLines(claimRepository.findLinesByClaimId(claim.getId()));
            Policy policy = policyRepository.findById(claim.getPolicyId());
            if (policy != null) {
                claim.setPolicyNumber(policy.getPolicyNumber());
            }
        }
        return claims;
    }

    /** Same N+1 shape, but with a user-supplied status filter (see repository SQLi note). */
    public List<Claim> searchClaimsForMember(long memberUserId, String status) {
        List<Claim> claims = claimRepository.searchByStatus(memberUserId, status);
        for (Claim claim : claims) {
            claim.setLines(claimRepository.findLinesByClaimId(claim.getId()));
            Policy policy = policyRepository.findById(claim.getPolicyId());
            if (policy != null) {
                claim.setPolicyNumber(policy.getPolicyNumber());
            }
        }
        return claims;
    }

    public Claim getClaim(long claimId) {
        Claim claim = claimRepository.findById(claimId);
        if (claim == null) {
            return null;
        }
        claim.setLines(claimRepository.findLinesByClaimId(claim.getId()));
        Policy policy = policyRepository.findById(claim.getPolicyId());
        if (policy != null) {
            claim.setPolicyNumber(policy.getPolicyNumber());
        }
        return claim;
    }
}
