package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Stock {
    private String stockName;
    private String stockTicker;
    private String market;
    private String sector;
    private BigDecimal lastPrice;
    private LocalDateTime lastUpdated;

    public Stock() {
    }

    public Stock(String stockName, String stockTicker, String market, String sector, BigDecimal lastPrice, LocalDateTime lastUpdated) {
        this.stockName = stockName;
        this.stockTicker = stockTicker;
        this.market = market;
        this.sector = sector;
        this.lastPrice = lastPrice;
        this.lastUpdated = lastUpdated;
    }

    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
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

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "Stock{" +
                "stockName='" + stockName + '\'' +
                ", stockTicker='" + stockTicker + '\'' +
                ", market='" + market + '\'' +
                ", sector='" + sector + '\'' +
                ", lastPrice=" + lastPrice +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
