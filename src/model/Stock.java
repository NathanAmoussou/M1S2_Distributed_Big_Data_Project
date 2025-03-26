package src.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Stock {
    private String stockId;
    private String stockName;
    private String stockTicker;
    private String market;
    private String sector;
    private BigDecimal lastPrice;
    private double price;
    private LocalDateTime lastUpdated;

    public Stock() {
    }

    public Stock(String stockId, String stockName, String stockTicker, String market, String sector, BigDecimal lastPrice, double price, LocalDateTime lastUpdated) {
        this.stockId = stockId;
        this.stockName = stockName;
        this.stockTicker = stockTicker;
        this.market = market;
        this.sector = sector;
        this.lastPrice = lastPrice;
        this.price = price;
        this.lastUpdated = lastUpdated;
    }

    public String getStockId() {
        return stockId;
    }

    public void setStockId(String stockId) {
        this.stockId = stockId;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public BigDecimal getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(BigDecimal lastPrice) {
        this.lastPrice = lastPrice;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "Stock{" +
                "stockId='" + stockId + '\'' +
                ", stockName='" + stockName + '\'' +
                ", stockTicker='" + stockTicker + '\'' +
                ", market='" + market + '\'' +
                ", sector='" + sector + '\'' +
                ", lastPrice=" + lastPrice +
                ", price=" + price +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
