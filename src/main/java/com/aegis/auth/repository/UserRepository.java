package com.aegis.auth.repository;

import com.aegis.auth.model.User;
import com.aegis.common.db.Database;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Raw-JDBC access to the {@code users} table. */
@Repository
public class UserRepository {

    private final Database db;

    @Autowired
    public UserRepository(Database db) {
        this.db = db;
    }

    public User findByUsername(String username) {
        // Parameterized here (login is the one place the original authors were careful).
        String sql = "SELECT id, username, password_hash, full_name, email, role "
                + "FROM users WHERE username = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUsername failed", e);
        }
    }

    public User findById(long id) {
        String sql = "SELECT id, username, password_hash, full_name, email, role "
                + "FROM users WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
    }

    /** Persists a new password hash for a user (used to upgrade legacy hashes). */
    public void updatePasswordHash(long id, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updatePasswordHash failed", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setRole(rs.getString("role"));
        return u;
    }
}
