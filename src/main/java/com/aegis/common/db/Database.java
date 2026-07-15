package com.aegis.common.db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Thin wrapper over the pooled {@link DataSource}. Repositories in this codebase
 * use raw JDBC against this connection provider rather than an ORM — this is
 * deliberate for the "no ORM in the hot paths" legacy story (see AGENTS.md).
 */
@Component
public class Database {

    private final DataSource dataSource;

    @Autowired
    public Database(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
