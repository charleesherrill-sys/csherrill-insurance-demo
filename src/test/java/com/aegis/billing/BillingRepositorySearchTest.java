package com.aegis.billing;

import com.aegis.billing.model.Invoice;
import com.aegis.billing.repository.BillingRepository;
import com.aegis.common.db.Database;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class BillingRepositorySearchTest {

    private BillingRepository repository;

    @Before
    public void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1");
        Database database = new Database(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("CREATE TABLE invoices ("
                    + "id BIGINT PRIMARY KEY, invoice_number VARCHAR, policy_id BIGINT, "
                    + "member_user_id BIGINT, amount_cents BIGINT, status VARCHAR, "
                    + "due_date DATE, created_at TIMESTAMP)");
            insert(connection, 1, 5583, "OPEN");
            insert(connection, 2, 5583, "PAID");
            insert(connection, 3, 4471, "OPEN");
        }
        repository = new BillingRepository(database);
    }

    @Test
    public void statusIsBoundAsAParameter() {
        assertEquals(0, repository.searchInvoices(5583, "' OR '1'='1").size());

        List<Invoice> open = repository.searchInvoices(5583, "OPEN");
        assertEquals(1, open.size());
        assertEquals(5583L, open.get(0).getMemberUserId());

        assertEquals(0, repository.searchInvoices(5583, "OPEN' OR '1'='1").size());
    }

    private void insert(Connection connection, long id, long memberUserId, String status) throws Exception {
        String sql = "INSERT INTO invoices (id, invoice_number, policy_id, member_user_id, "
                + "amount_cents, status, due_date, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setString(2, "INV-" + id);
            statement.setLong(3, 7001);
            statement.setLong(4, memberUserId);
            statement.setLong(5, 1000);
            statement.setString(6, status);
            statement.setDate(7, new java.sql.Date(System.currentTimeMillis()));
            statement.setTimestamp(8, new java.sql.Timestamp(System.currentTimeMillis()));
            statement.executeUpdate();
        }
    }
}
