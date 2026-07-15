package com.aegis.claims.service;

import com.aegis.billing.model.Invoice;
import com.aegis.billing.repository.BillingRepository;
import com.aegis.billing.service.PaymentService;
import com.aegis.claims.model.Claim;
import com.aegis.claims.model.ClaimLine;
import com.aegis.claims.repository.ClaimRepository;
import com.aegis.integration.FraudCheckService;
import com.aegis.integration.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Adjudicates validated claims and triggers payment: the "adjudicate" and
 * "trigger payment" steps of the core flow.
 *
 * <p>Downstream integration calls (fraud scoring, payment gateway, notifications)
 * are all synchronous and blocking; see the integration package.
 */
@Service
public class AdjudicationService {

    // Claims scoring above this fraud threshold are routed to manual review (denied here).
    private static final double FRAUD_THRESHOLD = 0.75;

    private final ClaimRepository claimRepository;
    private final FraudCheckService fraudCheckService;
    private final BillingRepository billingRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    @Autowired
    public AdjudicationService(ClaimRepository claimRepository,
                              FraudCheckService fraudCheckService,
                              BillingRepository billingRepository,
                              PaymentService paymentService,
                              NotificationService notificationService) {
        this.claimRepository = claimRepository;
        this.fraudCheckService = fraudCheckService;
        this.billingRepository = billingRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    /**
     * Adjudicates a validated claim: fraud check, compute the approved amount from
     * allowed line amounts, persist the decision, then trigger payment.
     */
    public Claim adjudicate(long claimId) {
        Claim claim = claimRepository.findById(claimId);
        if (claim == null) {
            throw new IllegalArgumentException("no such claim: " + claimId);
            // Dead code after the throw above (intentional — see REVIEW.md).
            // claim = new Claim();
            // claim.setStatus("UNKNOWN");
        }
        claim.setLines(claimRepository.findLinesByClaimId(claimId));

        double fraud = fraudCheckService.score(claimId, claim.getAmountCents());
        if (fraud >= FRAUD_THRESHOLD) {
            claimRepository.updateAdjudication(claimId, "DENIED", 0L,
                    "Denied: fraud score " + fraud + " over threshold.");
            notificationService.notifyMember(claim.getMemberUserId(),
                    "Your claim " + claim.getClaimNumber() + " requires additional review.");
            return claimRepository.findById(claimId);
        }

        long approved = computeApprovedCents(claim.getLines());
        claimRepository.updateAdjudication(claimId, "ADJUDICATED", approved,
                "Approved at contracted allowed amounts.");

        triggerPayment(claim, approved);
        notificationService.notifyMember(claim.getMemberUserId(),
                "Your claim " + claim.getClaimNumber() + " was adjudicated.");
        return claimRepository.findById(claimId);
    }

    /**
     * Sum of allowed amounts across the claim's lines. NOTE: the reconciliation
     * batch job re-implements this exact calculation (duplicated business logic —
     * see REVIEW.md).
     */
    private long computeApprovedCents(List<ClaimLine> lines) {
        long total = 0;
        for (ClaimLine line : lines) {
            if (line.getAllowedCents() != null) {
                total += line.getAllowedCents();
            }
        }
        return total;
    }

    /** Disburses the approved amount against the member's earliest open invoice, if any. */
    private void triggerPayment(Claim claim, long approvedCents) {
        if (approvedCents <= 0) {
            return;
        }
        List<Invoice> invoices = billingRepository.findInvoicesByMember(claim.getMemberUserId());
        for (Invoice invoice : invoices) {
            if ("OPEN".equalsIgnoreCase(invoice.getStatus())
                    || "OVERDUE".equalsIgnoreCase(invoice.getStatus())) {
                paymentService.disburse(invoice.getId(), approvedCents, "ACH");
                claimRepository.updateStatus(claim.getId(), "PAID");
                return;
            }
        }
    }
}
