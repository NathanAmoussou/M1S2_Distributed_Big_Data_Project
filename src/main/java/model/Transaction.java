package model;

import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private String transactionId;
    private int quantity;
    private BigDecimal priceAtTransaction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String stockId;
    private ObjectId walletId;
    private String transactionTypesId;
    private String transactionStatusId;

    public Transaction() {
    }

    public Transaction(String transactionId, int quantity, BigDecimal priceAtTransaction, LocalDateTime createdAt, LocalDateTime updatedAt, String stockId, ObjectId walletId, String transactionTypesId, String transactionStatusId) {
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
    public Transaction(JSONObject jsonObject) {
        this.transactionId = jsonObject.getString("transactionId");
        this.quantity = jsonObject.getInt("quantity");
        this.priceAtTransaction = new BigDecimal(jsonObject.getString("priceAtTransaction"));
        this.createdAt = LocalDateTime.parse(jsonObject.getString("createdAt"));
        this.updatedAt = LocalDateTime.parse(jsonObject.getString("updatedAt"));
        this.stockId = jsonObject.getString("stockId");
        this.walletId = new ObjectId(jsonObject.getString("walletId"));
        this.transactionTypesId = jsonObject.getString("transactionTypesId");
        this.transactionStatusId = jsonObject.getString("transactionStatusId");
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
        json.put("transactionId", transactionId);
        json.put("quantity", quantity);
        json.put("priceAtTransaction", priceAtTransaction.toString());
        json.put("createdAt", createdAt.toString());
        json.put("updatedAt", updatedAt.toString());
        json.put("stockId", stockId);
        json.put("walletId", walletId);
        json.put("transactionTypesId", transactionTypesId);
        json.put("transactionStatusId", transactionStatusId);
        return json;
    }

    public static Transaction fromJson(JSONObject json) {
        return new Transaction(
                json.getString("transactionId"),
                json.getInt("quantity"),
                new BigDecimal(json.getString("priceAtTransaction")),
                LocalDateTime.parse(json.getString("createdAt")),
                LocalDateTime.parse(json.getString("updatedAt")),
                json.getString("stockId"),
                new ObjectId(json.getString("walletId")),
                json.getString("transactionTypesId"),
                json.getString("transactionStatusId")
        );
    }
}
