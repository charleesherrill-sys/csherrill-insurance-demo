package com.aegis.billing.repository;

import com.aegis.common.db.Database;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for the SQL injection fix in
 * {@link BillingRepository#searchInvoices(long, String)} (CWE-89).
 *
 * <p>Proves the attacker-controlled {@code status} filter is bound as a query
 * parameter rather than concatenated into the SQL text, so an injection payload
 * cannot alter the query or escape the per-member scoping.
 */
public class BillingRepositorySearchInvoicesTest {

    @Test
    public void statusFilterIsParameterizedNotConcatenated() throws Exception {
        Database db = mock(Database.class);
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(db.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        String payload = "x' OR '1'='1";
        new BillingRepository(db).searchInvoices(42L, payload);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String executedSql = sqlCaptor.getValue();

        assertTrue("status must be bound with a placeholder",
                executedSql.contains("status = ?"));
        assertTrue("member scoping must be bound with a placeholder",
                executedSql.contains("member_user_id = ?"));
        assertFalse("raw payload must not appear in the SQL text",
                executedSql.contains(payload));
        assertFalse("status must not be spliced into a quoted literal",
                executedSql.contains("status = '"));

        // The attacker-controlled value reaches the driver as a bound parameter.
        verify(ps).setLong(eq(1), eq(42L));
        verify(ps).setString(eq(2), eq(payload));
        // The injectable raw Statement sink must never be used.
        verify(connection, never()).createStatement();
    }
}
