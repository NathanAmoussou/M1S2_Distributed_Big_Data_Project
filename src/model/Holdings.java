package src.model;

import java.math.BigDecimal;

public class Holdings {
    private String holdingsId;
    private String walletId;
    private String stockId;

    private int quantity;
    private BigDecimal averagePurchasePrice;

    public Holdings() {
    }

    public Holdings(String holdingsId, String walletId, String stockId, int quantity, BigDecimal averagePurchasePrice) {
        this.holdingsId = holdingsId;
        this.walletId = walletId;
        this.stockId = stockId;
        this.quantity = quantity;
        this.averagePurchasePrice = averagePurchasePrice;
    }

    public String getHoldingsId() {
        return holdingsId;
    }

    public void setHoldingsId(String holdingsId) {
        this.holdingsId = holdingsId;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getStockId() {
        return stockId;
    }

    public void setStockId(String stockId) {
        this.stockId = stockId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAveragePurchasePrice() {
        return averagePurchasePrice;
    }

    public void setAveragePurchasePrice(BigDecimal averagePurchasePrice) {
        this.averagePurchasePrice = averagePurchasePrice;
    }

    @Override
    public String toString() {
        return "Holdings{" +
                "holdingsId='" + holdingsId + '\'' +
                ", walletId='" + walletId + '\'' +
                ", stockId='" + stockId + '\'' +
                ", quantity=" + quantity +
                ", averagePurchasePrice=" + averagePurchasePrice +
                '}';
    }
}
