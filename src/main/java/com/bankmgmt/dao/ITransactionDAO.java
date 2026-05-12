package com.bankmgmt.dao;

import com.bankmgmt.model.BankTransaction;
import com.bankmgmt.model.TransactionType;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface ITransactionDAO {

    long insert(Connection conn, BankTransaction tx) throws SQLException;

    List<BankTransaction> findForUser(long userId, LocalDateTime from, LocalDateTime to, TransactionType typeFilter);

    List<BankTransaction> miniStatement(long accountId, int limitRows);

    List<BankTransaction> findAllLedger(LocalDateTime from, LocalDateTime to, String receiptOrDescContains);

    BankTransaction findByReceiptRef(String receiptRef);
}
