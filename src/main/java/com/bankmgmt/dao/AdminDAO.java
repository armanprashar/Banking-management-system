package com.bankmgmt.dao;

import com.bankmgmt.exceptions.DAOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminDAO extends BaseDAO implements IAdminDAO {

    @Override
    public boolean isAdmin(long userId) {
        String sql = "SELECT 1 FROM admins WHERE user_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DAOException("isAdmin failed", e);
        }
    }

    @Override
    public void linkAdmin(long userId) {
        String sql = "INSERT INTO admins (user_id) VALUES (?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("linkAdmin failed", e);
        }
    }
}
