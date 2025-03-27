package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private String transactionId;
    private int quantity;
    private BigDecimal priceAtTransaction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String stockId;
    private String walletId;
    private String transactionTypesId;
    private String transactionStatusId;

    public Transaction() {
    }

    public Transaction(String transactionId, int quantity, BigDecimal priceAtTransaction, LocalDateTime createdAt, LocalDateTime updatedAt, String stockId, String walletId, String transactionTypesId, String transactionStatusId) {
        this.transactionId = transactionId;
        this.quantity = quantity;
        this.priceAtTransaction = priceAtTransaction;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.stockId = stockId;
        this.walletId = walletId;
        this.transactionTypesId = transactionTypesId;
        this.transactionStatusId = transactionStatusId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPriceAtTransaction() {
        return priceAtTransaction;
    }

    public void setPriceAtTransaction(BigDecimal priceAtTransaction) {
        this.priceAtTransaction = priceAtTransaction;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStockId() {
        return stockId;
    }

    public void setStockId(String stockId) {
        this.stockId = stockId;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getTransactionTypesId() {
        return transactionTypesId;
    }

    public void setTransactionTypesId(String transactionTypesId) {
        this.transactionTypesId = transactionTypesId;
    }

    public String getTransactionStatusId() {
        return transactionStatusId;
    }

    public void setTransactionStatusId(String transactionStatusId) {
        this.transactionStatusId = transactionStatusId;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", quantity=" + quantity +
                ", priceAtTransaction=" + priceAtTransaction +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", stockId='" + stockId + '\'' +
                ", walletId='" + walletId + '\'' +
                ", transactionTypesId='" + transactionTypesId + '\'' +
                ", transactionStatusId='" + transactionStatusId + '\'' +
                '}';
    }
}
