package com.aegis.claims.repository;

import com.aegis.claims.model.Claim;
import com.aegis.claims.model.ClaimLine;
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

/** Raw-JDBC access to the {@code claims} and {@code claim_lines} tables. */
@Repository
public class ClaimRepository {

    private static final String BASE_COLUMNS =
            "id, claim_number, policy_id, member_user_id, claim_type, status, "
            + "amount_cents, approved_cents, diagnosis_code, adjudicator_notes, "
            + "submitted_at, adjudicated_at";

    private final Database db;

    @Autowired
    public ClaimRepository(Database db) {
        this.db = db;
    }

    /**
     * Loads a single claim by id. NOTE: this does NOT filter by member_user_id.
     * Callers are responsible for authorization; the claim-detail controller does
     * not perform that check (CWE-639). See REVIEW.md.
     */
    public Claim findById(long id) {
        String sql = "SELECT " + BASE_COLUMNS + " FROM claims WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("claim findById failed", e);
        }
    }

    public List<Claim> findByMember(long memberUserId) {
        String sql = "SELECT " + BASE_COLUMNS + " FROM claims "
                + "WHERE member_user_id = ? ORDER BY submitted_at DESC";
        List<Claim> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, memberUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("claim findByMember failed", e);
        }
        return out;
    }

    /**
     * Searches claims for a member with a status filter typed by the user.
     *
     * <p>The {@code memberUserId} and {@code status} values are bound as query
     * parameters (CWE-89 remediation), so user-supplied input cannot alter the
     * query structure.
     */
    public List<Claim> searchByStatus(long memberUserId, String status) {
        String sql = "SELECT " + BASE_COLUMNS + " FROM claims "
                + "WHERE member_user_id = ? AND status = ? "
                + "ORDER BY submitted_at DESC";
        List<Claim> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, memberUserId);
            ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("claim searchByStatus failed", e);
        }
        return out;
    }

    /** Loads the service lines for a single claim (called per-claim in the list path). */
    public List<ClaimLine> findLinesByClaimId(long claimId) {
        String sql = "SELECT id, claim_id, service_code, description, billed_cents, allowed_cents "
                + "FROM claim_lines WHERE claim_id = ? ORDER BY id";
        List<ClaimLine> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, claimId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ClaimLine l = new ClaimLine();
                    l.setId(rs.getLong("id"));
                    l.setClaimId(rs.getLong("claim_id"));
                    l.setServiceCode(rs.getString("service_code"));
                    l.setDescription(rs.getString("description"));
                    l.setBilledCents(rs.getLong("billed_cents"));
                    long allowed = rs.getLong("allowed_cents");
                    l.setAllowedCents(rs.wasNull() ? null : allowed);
                    out.add(l);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("claim findLinesByClaimId failed", e);
        }
        return out;
    }

    public long insert(Claim claim) {
        String sql = "INSERT INTO claims (id, claim_number, policy_id, member_user_id, "
                + "claim_type, status, amount_cents, diagnosis_code, submitted_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        long id = nextId();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, "CLM-" + id);
            ps.setLong(3, claim.getPolicyId());
            ps.setLong(4, claim.getMemberUserId());
            ps.setString(5, claim.getClaimType());
            ps.setString(6, "SUBMITTED");
            ps.setLong(7, claim.getAmountCents());
            ps.setString(8, claim.getDiagnosisCode());
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("claim insert failed", e);
        }
        return id;
    }

    public void updateAdjudication(long claimId, String status, Long approvedCents, String notes) {
        String sql = "UPDATE claims SET status = ?, approved_cents = ?, "
                + "adjudicator_notes = ?, adjudicated_at = ? WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            if (approvedCents == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, approvedCents);
            }
            ps.setString(3, notes);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(5, claimId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("claim updateAdjudication failed", e);
        }
    }

    public void updateStatus(long claimId, String status) {
        String sql = "UPDATE claims SET status = ? WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, claimId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("claim updateStatus failed", e);
        }
    }

    private long nextId() {
        try (Connection c = db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id), 90000) + 1 AS next FROM claims")) {
            return rs.next() ? rs.getLong("next") : 90001L;
        } catch (SQLException e) {
            throw new RuntimeException("claim nextId failed", e);
        }
    }

    private Claim map(ResultSet rs) throws SQLException {
        Claim c = new Claim();
        c.setId(rs.getLong("id"));
        c.setClaimNumber(rs.getString("claim_number"));
        c.setPolicyId(rs.getLong("policy_id"));
        c.setMemberUserId(rs.getLong("member_user_id"));
        c.setClaimType(rs.getString("claim_type"));
        c.setStatus(rs.getString("status"));
        c.setAmountCents(rs.getLong("amount_cents"));
        long approved = rs.getLong("approved_cents");
        c.setApprovedCents(rs.wasNull() ? null : approved);
        c.setDiagnosisCode(rs.getString("diagnosis_code"));
        c.setAdjudicatorNotes(rs.getString("adjudicator_notes"));
        Timestamp submitted = rs.getTimestamp("submitted_at");
        c.setSubmittedAt(submitted == null ? null : submitted.toLocalDateTime());
        Timestamp adjudicated = rs.getTimestamp("adjudicated_at");
        c.setAdjudicatedAt(adjudicated == null ? null : adjudicated.toLocalDateTime());
        return c;
    }
}
