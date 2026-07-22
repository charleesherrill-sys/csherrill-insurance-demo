package com.aegis.claims.repository;

import com.aegis.common.db.Database;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for the SQL injection remediation (CWE-89) in
 * {@link ClaimRepository#searchByStatus}.
 *
 * <p>Verifies the user-supplied {@code status} is bound as a
 * {@link PreparedStatement} parameter (treated as a literal value) rather than
 * concatenated into the SQL text, so a malicious status cannot inject SQL and
 * returns no unintended rows.
 */
public class ClaimRepositorySqlInjectionTest {

    private static final String MALICIOUS_STATUS = "' OR '1'='1";

    private Database db;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private ClaimRepository repository;

    @Before
    public void setUp() throws Exception {
        db = mock(Database.class);
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(db.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        repository = new ClaimRepository(db);
    }

    @Test
    public void maliciousStatusIsBoundAsLiteralParameter() throws Exception {
        assertTrue(repository.searchByStatus(42L, MALICIOUS_STATUS).isEmpty());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();

        // The query is parameterized: placeholders, not the injected literal.
        assertTrue("status must be a bound placeholder", sql.contains("status = ?"));
        assertTrue("member id must be a bound placeholder", sql.contains("member_user_id = ?"));
        assertFalse("malicious status must not appear in the SQL text",
                sql.contains(MALICIOUS_STATUS));
        assertFalse("SQL must not be built by concatenating the status literal",
                sql.contains("'"));

        // The malicious value is bound as a parameter (a literal), not executed.
        verify(preparedStatement).setLong(1, 42L);
        verify(preparedStatement).setString(2, MALICIOUS_STATUS);

        // No unparameterized Statement path is used for this query.
        verify(connection, never()).createStatement();
    }

    @Test
    public void ordinaryStatusStillWorks() throws Exception {
        assertEquals(0, repository.searchByStatus(7L, "SUBMITTED").size());
        verify(preparedStatement).setString(2, "SUBMITTED");
    }
}
