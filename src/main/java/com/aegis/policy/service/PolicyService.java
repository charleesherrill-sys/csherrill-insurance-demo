package com.aegis.policy.service;

import com.aegis.policy.model.Policy;
import com.aegis.policy.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/** Policy lookups used by the dashboard and the claims intake flow. */
@Service
public class PolicyService {

    private final PolicyRepository policyRepository;

    @Autowired
    public PolicyService(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public Policy getPolicy(long id) {
        return policyRepository.findById(id);
    }

    public List<Policy> getPoliciesForMember(long memberUserId) {
        return policyRepository.findByHolder(memberUserId);
    }

    /** A claim can only be filed against an active policy. */
    public boolean isPolicyActive(long policyId) {
        Policy p = policyRepository.findById(policyId);
        return p != null && p.isActive();
    }
}
