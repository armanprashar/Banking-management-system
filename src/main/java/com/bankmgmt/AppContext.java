package com.bankmgmt;

import com.bankmgmt.dao.*;
import com.bankmgmt.service.AdminService;
import com.bankmgmt.service.AuthService;
import com.bankmgmt.service.BankingService;

/**
 * Lightweight dependency wiring (composition root) for Swing presenters.
 */
public final class AppContext {

    private static final AppContext INSTANCE = new AppContext();

    private final IUserDAO userDAO = new UserDAO();
    private final IAdminDAO adminDAO = new AdminDAO();
    private final IAccountDAO accountDAO = new AccountDAO();
    private final ITransactionDAO transactionDAO = new TransactionDAO();
    private final IActivityLogDAO activityLogDAO = new ActivityLogDAO();

    private final AuthService authService;
    private final BankingService bankingService;
    private final AdminService adminService;

    private AppContext() {
        authService = new AuthService(userDAO, adminDAO, activityLogDAO);
        bankingService = new BankingService(accountDAO, transactionDAO, activityLogDAO);
        adminService = new AdminService(userDAO, accountDAO, transactionDAO, activityLogDAO, adminDAO);
    }

    public static AppContext get() {
        return INSTANCE;
    }

    public IUserDAO users() {
        return userDAO;
    }

    public IAdminDAO admins() {
        return adminDAO;
    }

    public IAccountDAO accounts() {
        return accountDAO;
    }

    public ITransactionDAO transactions() {
        return transactionDAO;
    }

    public IActivityLogDAO activityLogs() {
        return activityLogDAO;
    }

    public AuthService auth() {
        return authService;
    }

    public BankingService banking() {
        return bankingService;
    }

    public AdminService admin() {
        return adminService;
    }
}
