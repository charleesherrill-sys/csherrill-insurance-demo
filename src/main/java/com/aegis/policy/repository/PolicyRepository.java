package com.aegis.policy.repository;

import com.aegis.common.db.Database;
import com.aegis.policy.model.Policy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Raw-JDBC access to the {@code policies} table. */
@Repository
public class PolicyRepository {

    private final Database db;

    @Autowired
    public PolicyRepository(Database db) {
        this.db = db;
    }

    public Policy findById(long id) {
        String sql = "SELECT id, policy_number, holder_user_id, product, status, "
                + "premium_cents, effective_date, end_date FROM policies WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("policy findById failed", e);
        }
    }

    public List<Policy> findByHolder(long holderUserId) {
        String sql = "SELECT id, policy_number, holder_user_id, product, status, "
                + "premium_cents, effective_date, end_date FROM policies "
                + "WHERE holder_user_id = ? ORDER BY effective_date DESC";
        List<Policy> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, holderUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("policy findByHolder failed", e);
        }
        return out;
    }

    private Policy map(ResultSet rs) throws SQLException {
        Policy p = new Policy();
        p.setId(rs.getLong("id"));
        p.setPolicyNumber(rs.getString("policy_number"));
        p.setHolderUserId(rs.getLong("holder_user_id"));
        p.setProduct(rs.getString("product"));
        p.setStatus(rs.getString("status"));
        p.setPremiumCents(rs.getLong("premium_cents"));
        java.sql.Date eff = rs.getDate("effective_date");
        p.setEffectiveDate(eff == null ? null : eff.toLocalDate());
        java.sql.Date end = rs.getDate("end_date");
        p.setEndDate(end == null ? null : end.toLocalDate());
        return p;
    }
}
