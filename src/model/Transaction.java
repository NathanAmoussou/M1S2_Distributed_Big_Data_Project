package src.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private String transactionId;
    private int quantity;
    private BigDecimal priceAtTransaction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Stock stock;
    private Wallet wallet;
    private TransactionTypes transactionTypes;
    private TransactionStatus transactionStatus;

    public Transaction() {
    }

    public Transaction(String transactionId, int quantity, BigDecimal priceAtTransaction, LocalDateTime createdAt, LocalDateTime updatedAt, Stock stock, Wallet wallet, TransactionTypes transactionTypes, TransactionStatus transactionStatus) {
        this.transactionId = transactionId;
        this.quantity = quantity;
        this.priceAtTransaction = priceAtTransaction;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.stock = stock;
        this.wallet = wallet;
        this.transactionTypes = transactionTypes;
        this.transactionStatus = transactionStatus;
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

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public TransactionTypes getTransactionTypes() {
        return transactionTypes;
    }

    public void setTransactionTypes(TransactionTypes transactionTypes) {
        this.transactionTypes = transactionTypes;
    }

    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(TransactionStatus transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", quantity=" + quantity +
                ", priceAtTransaction=" + priceAtTransaction +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", stock=" + stock +
                ", wallet=" + wallet +
                ", transactionTypes=" + transactionTypes +
                ", transactionStatus=" + transactionStatus +
                '}';
    }
}
