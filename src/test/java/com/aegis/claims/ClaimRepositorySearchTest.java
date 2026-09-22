package com.aegis.claims;

import com.aegis.claims.model.Claim;
import com.aegis.claims.repository.ClaimRepository;
import com.aegis.common.db.Database;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ClaimRepositorySearchTest {

    private ClaimRepository repository;

    @Before
    public void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1");
        Database database = new Database(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("CREATE TABLE claims ("
                    + "id BIGINT PRIMARY KEY, claim_number VARCHAR, policy_id BIGINT, "
                    + "member_user_id BIGINT, claim_type VARCHAR, status VARCHAR, amount_cents BIGINT, "
                    + "approved_cents BIGINT NULL, diagnosis_code VARCHAR, adjudicator_notes VARCHAR, "
                    + "submitted_at TIMESTAMP, adjudicated_at TIMESTAMP NULL)");
            insert(connection, 1, 5583, "APPROVED");
            insert(connection, 2, 5583, "DENIED");
            insert(connection, 3, 4471, "APPROVED");
        }
        repository = new ClaimRepository(database);
    }

    @Test
    public void statusIsBoundAsAParameter() {
        assertEquals(0, repository.searchByStatus(5583, "' OR '1'='1").size());

        List<Claim> approved = repository.searchByStatus(5583, "APPROVED");
        assertEquals(1, approved.size());
        assertEquals(5583L, approved.get(0).getMemberUserId());

        assertEquals(0, repository.searchByStatus(5583, "APPROVED' OR '1'='1").size());
    }

    private void insert(Connection connection, long id, long memberUserId, String status) throws Exception {
        String sql = "INSERT INTO claims (id, claim_number, policy_id, member_user_id, claim_type, "
                + "status, amount_cents, approved_cents, diagnosis_code, adjudicator_notes, "
                + "submitted_at, adjudicated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setString(2, "CLM-" + id);
            statement.setLong(3, 7001);
            statement.setLong(4, memberUserId);
            statement.setString(5, "MEDICAL");
            statement.setString(6, status);
            statement.setLong(7, 1000);
            statement.setLong(8, 800);
            statement.setString(9, "R51");
            statement.setString(10, "notes");
            statement.setTimestamp(11, new java.sql.Timestamp(System.currentTimeMillis()));
            statement.setTimestamp(12, null);
            statement.executeUpdate();
        }
    }
}
