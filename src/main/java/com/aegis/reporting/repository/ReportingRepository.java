package com.aegis.reporting.repository;

import com.aegis.common.db.Database;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/** Aggregate queries for the reporting views. */
@Repository
public class ReportingRepository {

    private final Database db;

    @Autowired
    public ReportingRepository(Database db) {
        this.db = db;
    }

    public Map<String, Long> paidByClaimType() {
        Map<String, Long> out = new LinkedHashMap<>();
        String sql = "SELECT claim_type, COALESCE(SUM(approved_cents),0) AS total "
                + "FROM claims WHERE status IN ('ADJUDICATED','PAID') "
                + "GROUP BY claim_type ORDER BY claim_type";
        try (Connection c = db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.put(rs.getString("claim_type"), rs.getLong("total"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("paidByClaimType failed", e);
        }
        return out;
    }

    public long totalOutstandingInvoiceCents() {
        String sql = "SELECT COALESCE(SUM(amount_cents),0) AS total FROM invoices "
                + "WHERE status IN ('OPEN','OVERDUE')";
        try (Connection c = db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong("total") : 0L;
        } catch (SQLException e) {
            throw new RuntimeException("totalOutstandingInvoiceCents failed", e);
        }
    }
}
