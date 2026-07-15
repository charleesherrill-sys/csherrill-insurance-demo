package com.aegis.billing.service;

import com.aegis.billing.model.Invoice;
import com.aegis.billing.repository.BillingRepository;
import com.aegis.policy.model.Policy;
import com.aegis.policy.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Billing queries for the member billing view.
 *
 * <p>PERFORMANCE (INTENTIONAL — see REVIEW.md): {@link #getBillingForMember(long)}
 * has the same N+1 shape as the claims-list path. One query for the invoices, then
 * two more per invoice (payments + policy). This is the billing half of the
 * flagship "losing money" latency issue. Do NOT batch/join unless that is the task.
 */
@Service
public class BillingService {

    private final BillingRepository billingRepository;
    private final PolicyRepository policyRepository;

    @Autowired
    public BillingService(BillingRepository billingRepository, PolicyRepository policyRepository) {
        this.billingRepository = billingRepository;
        this.policyRepository = policyRepository;
    }

    public List<Invoice> getBillingForMember(long memberUserId) {
        List<Invoice> invoices = billingRepository.findInvoicesByMember(memberUserId);
        // N+1: two extra round-trips per invoice.
        for (Invoice invoice : invoices) {
            invoice.setPayments(billingRepository.findPaymentsByInvoice(invoice.getId()));
            Policy policy = policyRepository.findById(invoice.getPolicyId());
            if (policy != null) {
                invoice.setPolicyNumber(policy.getPolicyNumber());
            }
        }
        return invoices;
    }

    /**
     * Total outstanding across a member's invoices. Duplicates the per-invoice
     * outstanding math that also lives on {@link Invoice#getOutstandingCents()}
     * (intentional duplicated business logic — see REVIEW.md).
     */
    public long totalOutstandingCents(long memberUserId) {
        long total = 0;
        for (Invoice invoice : getBillingForMember(memberUserId)) {
            long settled = 0;
            for (com.aegis.billing.model.Payment p : invoice.getPayments()) {
                if ("SETTLED".equalsIgnoreCase(p.getStatus())) {
                    settled += p.getAmountCents();
                }
            }
            long outstanding = invoice.getAmountCents() - settled;
            if (outstanding > 0) {
                total += outstanding;
            }
        }
        return total;
    }
}
