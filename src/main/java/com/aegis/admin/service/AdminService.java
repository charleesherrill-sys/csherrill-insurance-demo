package com.aegis.admin.service;

import com.aegis.common.db.Database;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Backing queries for the admin portal. */
@Service
public class AdminService {

    private final Database db;

    @Autowired
    public AdminService(Database db) {
        this.db = db;
    }

    /** Lists all users, including their (MD5) password hashes and roles. */
    public List<Map<String, Object>> listAllUsers() {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = "SELECT id, username, full_name, email, role, password_hash FROM users ORDER BY id";
        try (Connection c = db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("username", rs.getString("username"));
                row.put("fullName", rs.getString("full_name"));
                row.put("email", rs.getString("email"));
                row.put("role", rs.getString("role"));
                row.put("passwordHash", rs.getString("password_hash"));
                out.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("listAllUsers failed", e);
        }
        return out;
    }

    public Map<String, Long> claimCountsByStatus() {
        Map<String, Long> counts = new LinkedHashMap<>();
        String sql = "SELECT status, COUNT(*) AS n FROM claims GROUP BY status ORDER BY status";
        try (Connection c = db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                counts.put(rs.getString("status"), rs.getLong("n"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("claimCountsByStatus failed", e);
        }
        return counts;
    }
}
