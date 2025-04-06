package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Stock {
    private String stockName; // ex "Apple Inc."
    private String stockTicker; // ex "AAPL"
    private String market; // ex "NMS"
    private String industry; // ex "Consumer Electronics"
    private String sector; // ex "Technology"
    private BigDecimal lastPrice; // ex 150.00
    private LocalDateTime lastUpdated;
    // On va peut-être les mettre ailleurs eux ?
    private String country; // ex "United States"
    private String currency; // ex "USD"


    public Stock() {
    }

    public Stock(
            String stockName,
            String stockTicker,
            String market,
            String industry,
            String sector,
            BigDecimal lastPrice,
            LocalDateTime lastUpdated,
            String country,
            String currency
    ) {
        this.stockName = stockName;
        this.stockTicker = stockTicker;
        this.market = market;
        this.industry = industry;
        this.sector = sector;
        this.lastPrice = lastPrice;
        this.lastUpdated = lastUpdated;
        this.country = country;
        this.currency = currency;
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

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
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

    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
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
                ", country='" + country + '\'' +
                ", currency='" + currency + '\'' +
                '}';
    }
}
