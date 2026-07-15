package com.aegis.batch;

import com.aegis.billing.model.Invoice;
import com.aegis.billing.model.Payment;
import com.aegis.billing.repository.BillingRepository;
import com.aegis.claims.model.Claim;
import com.aegis.claims.model.ClaimLine;
import com.aegis.claims.repository.ClaimRepository;
import com.aegis.common.db.Database;
import org.apache.commons.collections.map.LRUMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Nightly reconciliation: walks open invoices and their payments, recomputes
 * expected amounts, and records a run summary in {@code reconciliation_runs}.
 *
 * <p>Uses commons-collections {@link LRUMap} (the pinned vulnerable dependency)
 * as a small object cache. Also re-implements the adjudication "approved amount"
 * math (duplicated business logic — see REVIEW.md).
 */
@Service
public class ReconciliationService {

    private final BillingRepository billingRepository;
    private final ClaimRepository claimRepository;
    private final Database db;

    @SuppressWarnings("unchecked")
    private final LRUMap invoiceCache = new LRUMap(256);

    @Autowired
    public ReconciliationService(BillingRepository billingRepository,
                                ClaimRepository claimRepository,
                                Database db) {
        this.billingRepository = billingRepository;
        this.claimRepository = claimRepository;
        this.db = db;
    }

    @SuppressWarnings("unchecked")
    public ReconciliationResult run() {
        List<Invoice> open = billingRepository.findAllOpenInvoices();
        int matched = 0;
        int unmatched = 0;

        for (Invoice invoice : open) {
            invoiceCache.put(invoice.getId(), invoice);
            List<Payment> payments = billingRepository.findPaymentsByInvoice(invoice.getId());
            long settled = 0;
            for (Payment p : payments) {
                if ("SETTLED".equalsIgnoreCase(p.getStatus())) {
                    settled += p.getAmountCents();
                }
            }
            if (settled <= invoice.getAmountCents()) {
                matched++;
            } else {
                unmatched++;
            }
        }

        String status = unmatched == 0 ? "OK" : "DISCREPANCY";
        recordRun(status, matched, unmatched);
        return new ReconciliationResult(status, matched, unmatched);
    }

    /**
     * Duplicate of AdjudicationService's approved-amount calculation. Kept here so
     * the batch can independently value a claim (intentional duplication).
     */
    long expectedApprovedCents(long claimId) {
        Claim claim = claimRepository.findById(claimId);
        if (claim == null) {
            return 0;
        }
        long total = 0;
        for (ClaimLine line : claimRepository.findLinesByClaimId(claimId)) {
            if (line.getAllowedCents() != null) {
                total += line.getAllowedCents();
            }
        }
        return total;
    }

    private void recordRun(String status, int matched, int unmatched) {
        String sql = "INSERT INTO reconciliation_runs (run_date, status, matched_count, unmatched_count, notes) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, LocalDate.now());
            ps.setString(2, status);
            ps.setInt(3, matched);
            ps.setInt(4, unmatched);
            ps.setString(5, "nightly reconciliation");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("reconciliation record failed", e);
        }
    }

    /** Result of a reconciliation run. */
    public static class ReconciliationResult {
        private final String status;
        private final int matched;
        private final int unmatched;

        public ReconciliationResult(String status, int matched, int unmatched) {
            this.status = status;
            this.matched = matched;
            this.unmatched = unmatched;
        }

        public String getStatus() {
            return status;
        }

        public int getMatched() {
            return matched;
        }

        public int getUnmatched() {
            return unmatched;
        }
    }
}
