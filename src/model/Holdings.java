package src.model;

import java.math.BigDecimal;

public class Holdings {
    private String holdingsId;
    private Wallet wallet;
    private Stock stock;

    private int quantity;
    private BigDecimal averagePurchasePrice;

    public Holdings() {
    }

    public Holdings(String holdingsId, Wallet wallet, Stock stock, int quantity, BigDecimal averagePurchasePrice) {
        this.holdingsId = holdingsId;
        this.wallet = wallet;
        this.stock = stock;
        this.quantity = quantity;
        this.averagePurchasePrice = averagePurchasePrice;
    }

    public String getHoldingsId() {
        return holdingsId;
    }

    public void setHoldingsId(String holdingsId) {
        this.holdingsId = holdingsId;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
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
                ", wallet=" + wallet +
                ", stock=" + stock +
                ", quantity=" + quantity +
                ", averagePurchasePrice=" + averagePurchasePrice +
                '}';
    }
}
