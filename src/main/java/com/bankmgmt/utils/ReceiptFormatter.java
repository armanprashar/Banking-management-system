package com.bankmgmt.utils;

import com.bankmgmt.model.BankTransaction;

import java.time.format.DateTimeFormatter;

/** Builds printable receipt text from a persisted transaction row. */
public final class ReceiptFormatter {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ReceiptFormatter() {
    }

    public static String format(BankTransaction tx, String fromAcctNum, String toAcctNum) {
        StringBuilder sb = new StringBuilder();
        sb.append("========== BANK TRANSACTION RECEIPT ==========\n");
        sb.append("Receipt Ref : ").append(tx.getReceiptRef()).append('\n');
        sb.append("Date        : ").append(tx.getCreatedAt().format(FMT)).append('\n');
        sb.append("Type        : ").append(tx.getTransactionType()).append('\n');
        sb.append("Amount      : ").append(tx.getAmount()).append('\n');
        if (fromAcctNum != null) {
            sb.append("From Acct   : ").append(fromAcctNum).append('\n');
        }
        if (toAcctNum != null) {
            sb.append("To Acct     : ").append(toAcctNum).append('\n');
        }
        if (tx.getBalanceAfterFrom() != null) {
            sb.append("Bal after (from): ").append(tx.getBalanceAfterFrom()).append('\n');
        }
        if (tx.getBalanceAfterTo() != null) {
            sb.append("Bal after (to)  : ").append(tx.getBalanceAfterTo()).append('\n');
        }
        if (tx.getDescription() != null && !tx.getDescription().isBlank()) {
            sb.append("Note        : ").append(tx.getDescription()).append('\n');
        }
        sb.append("==============================================");
        return sb.toString();
    }
}
