package com.bankmgmt.dao;

import com.bankmgmt.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

/** Shared JDBC access point for concrete DAOs (template-style reuse). */
public abstract class BaseDAO {

    protected Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }
}
