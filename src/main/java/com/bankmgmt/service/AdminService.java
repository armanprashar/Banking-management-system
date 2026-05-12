package com.bankmgmt.service;

import com.bankmgmt.dao.IAccountDAO;
import com.bankmgmt.dao.IActivityLogDAO;
import com.bankmgmt.dao.IAdminDAO;
import com.bankmgmt.dao.ITransactionDAO;
import com.bankmgmt.dao.IUserDAO;
import com.bankmgmt.exceptions.BankException;
import com.bankmgmt.model.Account;
import com.bankmgmt.model.AccountStatus;
import com.bankmgmt.model.BankTransaction;
import com.bankmgmt.model.User;

import java.time.LocalDateTime;
import java.util.List;

/** Elevated operations guarded by UI role checks + admins registry. */
public class AdminService {

    private final IUserDAO userDAO;
    private final IAccountDAO accountDAO;
    private final ITransactionDAO transactionDAO;
    private final IActivityLogDAO activityLogDAO;
    private final IAdminDAO adminDAO;

    public AdminService(IUserDAO userDAO, IAccountDAO accountDAO,
                       ITransactionDAO transactionDAO,
                       IActivityLogDAO activityLogDAO, IAdminDAO adminDAO) {
        this.userDAO = userDAO;
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.activityLogDAO = activityLogDAO;
        this.adminDAO = adminDAO;
    }

    public void assertAdmin(User actor) throws BankException {
        if (actor == null || !adminDAO.isAdmin(actor.getId())) {
            throw new BankException("Administrator privileges required.");
        }
    }

    public List<User> allUsers() {
        return userDAO.findAll();
    }

    public List<User> searchUsers(String q) {
        return userDAO.search(q);
    }

    public void toggleUserActive(long targetUserId, boolean active, User actor) throws BankException {
        assertAdmin(actor);
        if (targetUserId == actor.getId()) {
            throw new BankException("You cannot change your own activation flag here.");
        }
        userDAO.updateActive(targetUserId, active);
        activityLogDAO.insert(actor.getId(), active ? "USER_ACTIVATED" : "USER_DEACTIVATED",
                "targetUserId=" + targetUserId);
    }

    public void setAccountStatus(long accountId, AccountStatus status, User actor) throws BankException {
        assertAdmin(actor);
        accountDAO.updateStatus(accountId, status);
        Account a = accountDAO.findById(accountId).orElse(null);
        activityLogDAO.insert(actor.getId(), "ACCOUNT_STATUS_CHANGED",
                accountId + " -> " + status + (a != null ? " (" + a.getAccountNumber() + ")" : ""));
    }

    public List<BankTransaction> ledger(LocalDateTime from, LocalDateTime to, String contains) {
        return transactionDAO.findAllLedger(from, to, contains);
    }

    public List<Account> searchAccounts(String partialNumber) {
        return accountDAO.searchByNumber(partialNumber);
    }

    public List<Account> allAccounts() {
        return accountDAO.findAll();
    }
}
