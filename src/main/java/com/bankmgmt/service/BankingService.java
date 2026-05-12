package com.bankmgmt.service;

import com.bankmgmt.dao.IAccountDAO;
import com.bankmgmt.dao.IActivityLogDAO;
import com.bankmgmt.dao.ITransactionDAO;
import com.bankmgmt.database.DatabaseConnection;
import com.bankmgmt.exceptions.BankException;
import com.bankmgmt.model.Account;
import com.bankmgmt.model.AccountStatus;
import com.bankmgmt.model.BankTransaction;
import com.bankmgmt.model.TransactionType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Core ledger workflows executed inside JDBC transactions.
 */
public class BankingService {

    private final IAccountDAO accountDAO;
    private final ITransactionDAO transactionDAO;
    private final IActivityLogDAO activityLogDAO;

    public BankingService(IAccountDAO accountDAO,
                         ITransactionDAO transactionDAO,
                         IActivityLogDAO activityLogDAO) {
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.activityLogDAO = activityLogDAO;
    }

    public Account openAccount(long userId, String accountTypeLabel) throws BankException {
        Account draft = new Account();
        draft.setUserId(userId);
        draft.setBalance(BigDecimal.ZERO);
        draft.setStatus(AccountStatus.ACTIVE);
        draft.setAccountType(accountTypeLabel != null ? accountTypeLabel : "SAVINGS");
        Account saved = accountDAO.insert(draft);
        activityLogDAO.insert(userId, "ACCOUNT_OPENED", saved.getAccountNumber());
        return saved;
    }

    public BankTransaction deposit(long accountId, long performingUserId, BigDecimal amount, String note)
            throws BankException {
        Account acc = accountDAO.findById(accountId).orElseThrow(() -> new BankException("Account not found."));
        ensureOwner(acc, performingUserId);
        ensureTradeable(acc);

        String receiptRef = UUID.randomUUID().toString();
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal newBal = acc.getBalance().add(amount);
                accountDAO.updateBalance(conn, accountId, newBal);

                BankTransaction tx = new BankTransaction();
                tx.setFromAccountId(null);
                tx.setToAccountId(accountId);
                tx.setAmount(amount);
                tx.setTransactionType(TransactionType.DEPOSIT);
                tx.setDescription(note);
                tx.setBalanceAfterFrom(null);
                tx.setBalanceAfterTo(newBal);
                tx.setPerformedByUserId(performingUserId);
                tx.setReceiptRef(receiptRef);

                transactionDAO.insert(conn, tx);
                conn.commit();
                activityLogDAO.insert(performingUserId, "DEPOSIT", acc.getAccountNumber() + " +" + amount);
                return transactionDAO.findByReceiptRef(receiptRef);
            } catch (Exception ex) {
                conn.rollback();
                throw wrap(ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new BankException("Deposit failed", e);
        }
    }

    public BankTransaction withdraw(long accountId, long performingUserId, BigDecimal amount, String note)
            throws BankException {
        Account acc = accountDAO.findById(accountId).orElseThrow(() -> new BankException("Account not found."));
        ensureOwner(acc, performingUserId);
        ensureTradeable(acc);
        if (acc.getBalance().compareTo(amount) < 0) {
            throw new BankException("Insufficient funds.");
        }

        String receiptRef = UUID.randomUUID().toString();
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal newBal = acc.getBalance().subtract(amount);
                accountDAO.updateBalance(conn, accountId, newBal);

                BankTransaction tx = new BankTransaction();
                tx.setFromAccountId(accountId);
                tx.setToAccountId(null);
                tx.setAmount(amount);
                tx.setTransactionType(TransactionType.WITHDRAW);
                tx.setDescription(note);
                tx.setBalanceAfterFrom(newBal);
                tx.setBalanceAfterTo(null);
                tx.setPerformedByUserId(performingUserId);
                tx.setReceiptRef(receiptRef);

                transactionDAO.insert(conn, tx);
                conn.commit();
                activityLogDAO.insert(performingUserId, "WITHDRAW", acc.getAccountNumber() + " -" + amount);
                return transactionDAO.findByReceiptRef(receiptRef);
            } catch (Exception ex) {
                conn.rollback();
                throw wrap(ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new BankException("Withdraw failed", e);
        }
    }

    public BankTransaction transfer(long fromAccountId, String toAccountNumber, long performingUserId,
                                   BigDecimal amount, String note) throws BankException {
        Account from = accountDAO.findById(fromAccountId).orElseThrow(() -> new BankException("Source account invalid."));
        Account to = accountDAO.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new BankException("Destination account not found."));
        if (from.getId() == to.getId()) {
            throw new BankException("Cannot transfer to the same account.");
        }
        ensureOwner(from, performingUserId);
        ensureTradeable(from);
        ensureTradeable(to);
        if (from.getBalance().compareTo(amount) < 0) {
            throw new BankException("Insufficient funds.");
        }

        String receiptRef = UUID.randomUUID().toString();
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal newFrom = from.getBalance().subtract(amount);
                BigDecimal newTo = to.getBalance().add(amount);
                accountDAO.updateBalance(conn, from.getId(), newFrom);
                accountDAO.updateBalance(conn, to.getId(), newTo);

                BankTransaction tx = new BankTransaction();
                tx.setFromAccountId(from.getId());
                tx.setToAccountId(to.getId());
                tx.setAmount(amount);
                tx.setTransactionType(TransactionType.TRANSFER);
                tx.setDescription(note);
                tx.setBalanceAfterFrom(newFrom);
                tx.setBalanceAfterTo(newTo);
                tx.setPerformedByUserId(performingUserId);
                tx.setReceiptRef(receiptRef);

                transactionDAO.insert(conn, tx);
                conn.commit();
                activityLogDAO.insert(performingUserId, "TRANSFER",
                        from.getAccountNumber() + " -> " + to.getAccountNumber() + " " + amount);
                return transactionDAO.findByReceiptRef(receiptRef);
            } catch (Exception ex) {
                conn.rollback();
                throw wrap(ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new BankException("Transfer failed", e);
        }
    }

    public List<BankTransaction> history(long userId,
                                       java.time.LocalDateTime from,
                                       java.time.LocalDateTime to,
                                       com.bankmgmt.model.TransactionType typeFilter) {
        return transactionDAO.findForUser(userId, from, to, typeFilter);
    }

    public List<BankTransaction> mini(long accountId, int rows) {
        return transactionDAO.miniStatement(accountId, rows);
    }

    private static void ensureOwner(Account acc, long userId) throws BankException {
        if (acc.getUserId() != userId) {
            throw new BankException("You do not own this account.");
        }
    }

    private static void ensureTradeable(Account acc) throws BankException {
        if (acc.getStatus() != AccountStatus.ACTIVE) {
            throw new BankException("Account is not active for transactions.");
        }
    }

    private static BankException wrap(Exception ex) {
        if (ex instanceof BankException be) {
            return be;
        }
        return new BankException("Operation aborted", ex);
    }
}
