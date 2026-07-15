package com.aegis.document.repository;

import com.aegis.common.db.Database;
import com.aegis.document.model.Document;
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

/** Raw-JDBC access to the {@code documents} table. */
@Repository
public class DocumentRepository {

    private static final String COLUMNS =
            "id, claim_id, owner_user_id, filename, stored_path, content_type, uploaded_at";

    private final Database db;

    @Autowired
    public DocumentRepository(Database db) {
        this.db = db;
    }

    public List<Document> findByClaim(long claimId) {
        String sql = "SELECT " + COLUMNS + " FROM documents WHERE claim_id = ? ORDER BY uploaded_at";
        List<Document> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, claimId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("document findByClaim failed", e);
        }
        return out;
    }

    public long insert(Document doc) {
        long id = nextId();
        String sql = "INSERT INTO documents (id, claim_id, owner_user_id, filename, stored_path, content_type, uploaded_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            if (doc.getClaimId() == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, doc.getClaimId());
            }
            ps.setLong(3, doc.getOwnerUserId());
            ps.setString(4, doc.getFilename());
            ps.setString(5, doc.getStoredPath());
            ps.setString(6, doc.getContentType());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("document insert failed", e);
        }
        return id;
    }

    private long nextId() {
        try (Connection c = db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id), 5000) + 1 AS next FROM documents")) {
            return rs.next() ? rs.getLong("next") : 5001L;
        } catch (SQLException e) {
            throw new RuntimeException("document nextId failed", e);
        }
    }

    private Document map(ResultSet rs) throws SQLException {
        Document d = new Document();
        d.setId(rs.getLong("id"));
        long claimId = rs.getLong("claim_id");
        d.setClaimId(rs.wasNull() ? null : claimId);
        d.setOwnerUserId(rs.getLong("owner_user_id"));
        d.setFilename(rs.getString("filename"));
        d.setStoredPath(rs.getString("stored_path"));
        d.setContentType(rs.getString("content_type"));
        Timestamp uploaded = rs.getTimestamp("uploaded_at");
        d.setUploadedAt(uploaded == null ? null : uploaded.toLocalDateTime());
        return d;
    }
}
