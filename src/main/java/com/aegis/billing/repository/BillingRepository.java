package com.aegis.billing.repository;

import com.aegis.billing.model.Invoice;
import com.aegis.billing.model.Payment;
import com.aegis.common.db.Database;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Raw-JDBC access to the {@code invoices} and {@code payments} tables. */
@Repository
public class BillingRepository {

    private static final String INVOICE_COLUMNS =
            "id, invoice_number, policy_id, member_user_id, amount_cents, status, due_date, created_at";

    private final Database db;

    @Autowired
    public BillingRepository(Database db) {
        this.db = db;
    }

    public List<Invoice> findInvoicesByMember(long memberUserId) {
        String sql = "SELECT " + INVOICE_COLUMNS + " FROM invoices "
                + "WHERE member_user_id = ? ORDER BY created_at DESC";
        List<Invoice> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, memberUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapInvoice(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findInvoicesByMember failed", e);
        }
        return out;
    }

    /**
     * Finds invoices for a member with a status filter.
     *
     * <p>Both {@code memberUserId} and the caller-supplied {@code status} are bound
     * as query parameters so the filter cannot be used for SQL injection (CWE-89)
     * or to escape the per-member scoping, mirroring {@link #findInvoicesByMember(long)}.
     */
    public List<Invoice> searchInvoices(long memberUserId, String status) {
        String sql = "SELECT " + INVOICE_COLUMNS + " FROM invoices "
                + "WHERE member_user_id = ? AND status = ? ORDER BY created_at DESC";
        List<Invoice> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, memberUserId);
            ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapInvoice(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("searchInvoices failed", e);
        }
        return out;
    }

    public Invoice findInvoiceById(long id) {
        String sql = "SELECT " + INVOICE_COLUMNS + " FROM invoices WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapInvoice(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findInvoiceById failed", e);
        }
    }

    /** Loads the payments for one invoice (called per-invoice in the billing path). */
    public List<Payment> findPaymentsByInvoice(long invoiceId) {
        String sql = "SELECT id, invoice_id, amount_cents, method, status, external_ref, created_at "
                + "FROM payments WHERE invoice_id = ? ORDER BY created_at";
        List<Payment> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapPayment(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findPaymentsByInvoice failed", e);
        }
        return out;
    }

    public long insertPayment(Payment payment) {
        long id = nextPaymentId();
        String sql = "INSERT INTO payments (id, invoice_id, amount_cents, method, status, external_ref, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, payment.getInvoiceId());
            ps.setLong(3, payment.getAmountCents());
            ps.setString(4, payment.getMethod());
            ps.setString(5, payment.getStatus());
            ps.setString(6, payment.getExternalRef());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insertPayment failed", e);
        }
        return id;
    }

    public void updateInvoiceStatus(long invoiceId, String status) {
        String sql = "UPDATE invoices SET status = ? WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, invoiceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateInvoiceStatus failed", e);
        }
    }

    public List<Invoice> findAllOpenInvoices() {
        String sql = "SELECT " + INVOICE_COLUMNS + " FROM invoices "
                + "WHERE status IN ('OPEN','OVERDUE') ORDER BY due_date";
        List<Invoice> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(mapInvoice(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAllOpenInvoices failed", e);
        }
        return out;
    }

    private long nextPaymentId() {
        try (Connection c = db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id), 9000) + 1 AS next FROM payments")) {
            return rs.next() ? rs.getLong("next") : 9001L;
        } catch (SQLException e) {
            throw new RuntimeException("nextPaymentId failed", e);
        }
    }

    private Invoice mapInvoice(ResultSet rs) throws SQLException {
        Invoice i = new Invoice();
        i.setId(rs.getLong("id"));
        i.setInvoiceNumber(rs.getString("invoice_number"));
        i.setPolicyId(rs.getLong("policy_id"));
        i.setMemberUserId(rs.getLong("member_user_id"));
        i.setAmountCents(rs.getLong("amount_cents"));
        i.setStatus(rs.getString("status"));
        java.sql.Date due = rs.getDate("due_date");
        i.setDueDate(due == null ? null : due.toLocalDate());
        Timestamp created = rs.getTimestamp("created_at");
        i.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return i;
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getLong("id"));
        p.setInvoiceId(rs.getLong("invoice_id"));
        p.setAmountCents(rs.getLong("amount_cents"));
        p.setMethod(rs.getString("method"));
        p.setStatus(rs.getString("status"));
        p.setExternalRef(rs.getString("external_ref"));
        Timestamp created = rs.getTimestamp("created_at");
        p.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return p;
    }
}
