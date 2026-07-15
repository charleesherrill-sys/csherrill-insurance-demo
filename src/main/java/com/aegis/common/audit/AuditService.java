package com.aegis.common.audit;

import com.aegis.common.db.Database;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Appends rows to {@code audit_log}. The claim-detail endpoint records a
 * "CLAIM_VIEW" entry here; that access log is what surfaces the cross-account
 * reads described in demo/trigger-artifact.md.
 */
@Service
public class AuditService {

    private final Database db;

    @Autowired
    public AuditService(Database db) {
        this.db = db;
    }

    public void record(Long actorUserId, String action, String entity, String entityId, String detail) {
        String sql = "INSERT INTO audit_log (actor_user_id, action, entity, entity_id, detail) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (actorUserId == null) {
                ps.setNull(1, java.sql.Types.BIGINT);
            } else {
                ps.setLong(1, actorUserId);
            }
            ps.setString(2, action);
            ps.setString(3, entity);
            ps.setString(4, entityId);
            ps.setString(5, detail);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Audit failures should not break the request.
            System.err.println("audit write failed: " + e.getMessage());
        }
    }
}
