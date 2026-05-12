package com.bankmgmt.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ledger row — distinct name avoids clash with {@link java.sql.Connection}. */
public class BankTransaction extends AbstractEntity {

    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private TransactionType transactionType;
    private String description;
    private BigDecimal balanceAfterFrom;
    private BigDecimal balanceAfterTo;
    private long performedByUserId;
    private String receiptRef;
    private LocalDateTime createdAt;

    public Long getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(Long fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBalanceAfterFrom() {
        return balanceAfterFrom;
    }

    public void setBalanceAfterFrom(BigDecimal balanceAfterFrom) {
        this.balanceAfterFrom = balanceAfterFrom;
    }

    public BigDecimal getBalanceAfterTo() {
        return balanceAfterTo;
    }

    public void setBalanceAfterTo(BigDecimal balanceAfterTo) {
        this.balanceAfterTo = balanceAfterTo;
    }

    public long getPerformedByUserId() {
        return performedByUserId;
    }

    public void setPerformedByUserId(long performedByUserId) {
        this.performedByUserId = performedByUserId;
    }

    public String getReceiptRef() {
        return receiptRef;
    }

    public void setReceiptRef(String receiptRef) {
        this.receiptRef = receiptRef;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
