package com.bankmgmt.dao;

import com.bankmgmt.model.Account;
import com.bankmgmt.model.AccountStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface IAccountDAO {

    Optional<Account> findById(long accountId);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByUserId(long userId);

    /** Creates account with generated unique number; returns persisted entity with id set. */
    Account insert(Account draft) throws com.bankmgmt.exceptions.BankException;

    void updateStatus(long accountId, AccountStatus status);

    void updateBalance(Connection conn, long accountId, BigDecimal newBalance) throws Exception;

    List<Account> searchByNumber(String partialNumber);

    List<Account> findAll();
}
