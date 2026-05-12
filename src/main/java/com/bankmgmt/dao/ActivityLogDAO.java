package com.bankmgmt.dao;

import com.bankmgmt.exceptions.DAOException;
import com.bankmgmt.model.ActivityLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAO extends BaseDAO implements IActivityLogDAO {

    private static ActivityLog map(ResultSet rs) throws SQLException {
        ActivityLog a = new ActivityLog();
        a.setId(rs.getLong("id"));
        long uid = rs.getLong("user_id");
        a.setUserId(rs.wasNull() ? null : uid);
        a.setAction(rs.getString("action"));
        a.setDetails(rs.getString("details"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            a.setCreatedAt(ts.toLocalDateTime());
        }
        return a;
    }

    @Override
    public void insert(Long userId, String action, String details) {
        String sql = "INSERT INTO activity_logs (user_id, action, details) VALUES (?,?,?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (userId != null) {
                ps.setLong(1, userId);
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("activity log insert failed", e);
        }
    }

    @Override
    public List<ActivityLog> recent(int limit) {
        String sql = "SELECT * FROM activity_logs ORDER BY created_at DESC LIMIT ?";
        List<ActivityLog> list = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("activity recent failed", e);
        }
        return list;
    }

    @Override
    public List<ActivityLog> filter(String actionContains) {
        String sql = """
                SELECT * FROM activity_logs
                WHERE action LIKE ?
                ORDER BY created_at DESC
                LIMIT 2000
                """;
        List<ActivityLog> list = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + actionContains.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("activity filter failed", e);
        }
        return list;
    }
}
