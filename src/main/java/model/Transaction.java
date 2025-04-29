package model;

import org.bson.types.ObjectId;
import org.json.JSONObject;
import util.JsonUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private ObjectId transactionId; // MongoDB ObjectId optional since if not in the json mongodb will generate it
    private BigDecimal quantity;
    private BigDecimal priceAtTransaction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String stockId;
    private ObjectId walletId;
    private String transactionTypesId;
    private String transactionStatusId;

    public Transaction() {
    }

    public Transaction(ObjectId transactionId, BigDecimal quantity, BigDecimal priceAtTransaction, LocalDateTime createdAt, LocalDateTime updatedAt, String stockId, ObjectId walletId, String transactionTypesId, String transactionStatusId) {
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

    // Constructor for JSON
    public Transaction(JSONObject json) {
        this.transactionId = JsonUtils.getObjectId(json, "_id");
        this.quantity = JsonUtils.getBigDecimal(json, "quantity");
        this.priceAtTransaction = new BigDecimal(json.getString("priceAtTransaction"));
        this.createdAt = LocalDateTime.parse(json.getString("createdAt"));
        this.updatedAt = LocalDateTime.parse(json.getString("updatedAt"));
        this.stockId = json.getString("stockId");
        this.walletId = new ObjectId(json.getString("walletId"));
        this.transactionTypesId = json.getString("transactionTypesId");
        this.transactionStatusId = json.getString("transactionStatusId");
    }

    public ObjectId getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(ObjectId transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
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

    public ObjectId getWalletId() {
        return walletId;
    }

    public void setWalletId(ObjectId walletId) {
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
        JSONObject jsonObject = this.toJson();
        return jsonObject.toString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("_id", transactionId);
        json.put("quantity", quantity);
        json.put("priceAtTransaction", priceAtTransaction);
        json.put("createdAt", createdAt);
        json.put("updatedAt", updatedAt);
        json.put("stockId", stockId);
        json.put("walletId", walletId);
        json.put("transactionTypesId", transactionTypesId);
        json.put("transactionStatusId", transactionStatusId);
        return json;
    }

}
