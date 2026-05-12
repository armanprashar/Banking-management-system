package com.bankmgmt.dao;

import com.bankmgmt.exceptions.BankException;
import com.bankmgmt.exceptions.DAOException;
import com.bankmgmt.model.Account;
import com.bankmgmt.model.AccountStatus;
import com.bankmgmt.utils.AccountNumberGenerator;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountDAO extends BaseDAO implements IAccountDAO {

    private static Account map(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setId(rs.getLong("id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setUserId(rs.getLong("user_id"));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setStatus(AccountStatus.valueOf(rs.getString("status")));
        a.setAccountType(rs.getString("account_type"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            a.setCreatedAt(ts.toLocalDateTime());
        }
        return a;
    }

    @Override
    public Optional<Account> findById(long accountId) {
        String sql = "SELECT * FROM accounts WHERE id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("findById account failed", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accountNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("findByAccountNumber failed", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Account> findByUserId(long userId) {
        String sql = "SELECT * FROM accounts WHERE user_id = ? ORDER BY created_at DESC";
        List<Account> list = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("findByUserId accounts failed", e);
        }
        return list;
    }

    @Override
    public Account insert(Account draft) throws BankException {
        final int maxAttempts = 40;
        SQLException last = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String number = AccountNumberGenerator.generateCandidate();
            String sql = """
                    INSERT INTO accounts (account_number, user_id, balance, status, account_type)
                    VALUES (?,?,?,?,?)
                    """;
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, number);
                ps.setLong(2, draft.getUserId());
                ps.setBigDecimal(3, draft.getBalance() != null ? draft.getBalance() : BigDecimal.ZERO);
                ps.setString(4, draft.getStatus().name());
                ps.setString(5, draft.getAccountType() != null ? draft.getAccountType() : "SAVINGS");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        draft.setId(keys.getLong(1));
                        draft.setAccountNumber(number);
                        Timestamp ts = Timestamp.valueOf(LocalDateTime.now());
                        draft.setCreatedAt(ts.toLocalDateTime());
                        return draft;
                    }
                }
            } catch (SQLIntegrityConstraintViolationException ex) {
                last = ex;
            } catch (SQLException e) {
                throw new DAOException("insert account failed", e);
            }
        }
        throw new BankException("Could not allocate unique account number", last);
    }

    @Override
    public void updateStatus(long accountId, AccountStatus status) {
        String sql = "UPDATE accounts SET status = ? WHERE id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("updateStatus failed", e);
        }
    }

    @Override
    public void updateBalance(Connection conn, long accountId, BigDecimal newBalance) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setLong(2, accountId);
            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("Balance update affected " + n + " rows");
            }
        }
    }

    @Override
    public List<Account> searchByNumber(String partialNumber) {
        String sql = "SELECT * FROM accounts WHERE account_number LIKE ? ORDER BY created_at DESC";
        List<Account> list = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + partialNumber.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("searchByNumber failed", e);
        }
        return list;
    }

    @Override
    public List<Account> findAll() {
        String sql = "SELECT * FROM accounts ORDER BY created_at DESC";
        List<Account> list = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DAOException("findAll accounts failed", e);
        }
        return list;
    }
}
