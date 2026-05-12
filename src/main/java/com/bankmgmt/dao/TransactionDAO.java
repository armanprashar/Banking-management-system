package com.bankmgmt.dao;

import com.bankmgmt.exceptions.DAOException;
import com.bankmgmt.model.BankTransaction;
import com.bankmgmt.model.TransactionType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO extends BaseDAO implements ITransactionDAO {

    private static BankTransaction map(ResultSet rs) throws SQLException {
        BankTransaction t = new BankTransaction();
        t.setId(rs.getLong("id"));
        long fid = rs.getLong("from_account_id");
        t.setFromAccountId(rs.wasNull() ? null : fid);
        long tid = rs.getLong("to_account_id");
        t.setToAccountId(rs.wasNull() ? null : tid);
        t.setAmount(rs.getBigDecimal("amount"));
        t.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        t.setDescription(rs.getString("description"));
        BigDecimal baf = rs.getBigDecimal("balance_after_from");
        t.setBalanceAfterFrom(rs.wasNull() ? null : baf);
        BigDecimal bat = rs.getBigDecimal("balance_after_to");
        t.setBalanceAfterTo(rs.wasNull() ? null : bat);
        t.setPerformedByUserId(rs.getLong("performed_by_user_id"));
        t.setReceiptRef(rs.getString("receipt_ref"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            t.setCreatedAt(ts.toLocalDateTime());
        }
        return t;
    }

    @Override
    public long insert(Connection conn, BankTransaction tx) throws SQLException {
        String sql = """
                INSERT INTO transactions (
                  from_account_id, to_account_id, amount, transaction_type, description,
                  balance_after_from, balance_after_to, performed_by_user_id, receipt_ref
                ) VALUES (?,?,?,?,?,?,?,?,?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (tx.getFromAccountId() != null) {
                ps.setLong(1, tx.getFromAccountId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            if (tx.getToAccountId() != null) {
                ps.setLong(2, tx.getToAccountId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setBigDecimal(3, tx.getAmount());
            ps.setString(4, tx.getTransactionType().name());
            ps.setString(5, tx.getDescription());
            if (tx.getBalanceAfterFrom() != null) {
                ps.setBigDecimal(6, tx.getBalanceAfterFrom());
            } else {
                ps.setNull(6, Types.DECIMAL);
            }
            if (tx.getBalanceAfterTo() != null) {
                ps.setBigDecimal(7, tx.getBalanceAfterTo());
            } else {
                ps.setNull(7, Types.DECIMAL);
            }
            ps.setLong(8, tx.getPerformedByUserId());
            ps.setString(9, tx.getReceiptRef());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("insert transaction: no generated key");
    }

    @Override
    public List<BankTransaction> findForUser(long userId, LocalDateTime from, LocalDateTime to,
                                             TransactionType typeFilter) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.* FROM transactions t
                WHERE (
                  t.from_account_id IN (SELECT id FROM accounts WHERE user_id = ?)
                  OR t.to_account_id IN (SELECT id FROM accounts WHERE user_id = ?)
                )
                """);
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(userId);
        if (from != null) {
            sql.append(" AND t.created_at >= ? ");
            params.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND t.created_at <= ? ");
            params.add(Timestamp.valueOf(to));
        }
        if (typeFilter != null) {
            sql.append(" AND t.transaction_type = ? ");
            params.add(typeFilter.name());
        }
        sql.append(" ORDER BY t.created_at DESC ");

        List<BankTransaction> list = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("findForUser transactions failed", e);
        }
        return list;
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        int i = 1;
        for (Object p : params) {
            ps.setObject(i++, p);
        }
    }

    @Override
    public List<BankTransaction> miniStatement(long accountId, int limitRows) {
        String sql = """
                SELECT * FROM transactions
                WHERE from_account_id = ? OR to_account_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        List<BankTransaction> list = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setLong(2, accountId);
            ps.setInt(3, limitRows);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("miniStatement failed", e);
        }
        return list;
    }

    @Override
    public List<BankTransaction> findAllLedger(LocalDateTime from, LocalDateTime to, String contains) {
        StringBuilder sql = new StringBuilder("SELECT * FROM transactions WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (from != null) {
            sql.append(" AND created_at >= ? ");
            params.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND created_at <= ? ");
            params.add(Timestamp.valueOf(to));
        }
        if (contains != null && !contains.isBlank()) {
            sql.append(" AND (receipt_ref LIKE ? OR description LIKE ?) ");
            String wild = "%" + contains.trim() + "%";
            params.add(wild);
            params.add(wild);
        }
        sql.append(" ORDER BY created_at DESC LIMIT 5000 ");

        List<BankTransaction> list = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("findAllLedger failed", e);
        }
        return list;
    }

    @Override
    public BankTransaction findByReceiptRef(String receiptRef) {
        String sql = "SELECT * FROM transactions WHERE receipt_ref = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, receiptRef.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("findByReceiptRef failed", e);
        }
        return null;
    }
}
