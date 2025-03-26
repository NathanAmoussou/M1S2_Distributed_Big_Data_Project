package src.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockPriceHistory {
    private String stockPriceHistoryId;
    private String stockId;
    private LocalDateTime dateTime;
    private BigDecimal openPrice;
    private BigDecimal closePrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;

    public StockPriceHistory() {
    }

    public StockPriceHistory(String stockPriceHistoryId, String stockId, LocalDateTime dateTime, BigDecimal openPrice, BigDecimal closePrice, BigDecimal highPrice, BigDecimal lowPrice) {
        this.stockPriceHistoryId = stockPriceHistoryId;
        this.stockId = stockId;
        this.dateTime = dateTime;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
    }

    public String getStockPriceHistoryId() {
        return stockPriceHistoryId;
    }

    public void setStockPriceHistoryId(String stockPriceHistoryId) {
        this.stockPriceHistoryId = stockPriceHistoryId;
    }

    public String getStockId() {
        return stockId;
    }

    public void setStockId(String stockId) {
        this.stockId = stockId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(BigDecimal lowPrice) {
        this.lowPrice = lowPrice;
    }

    @Override
    public String toString() {
        return "StockPriceHistory{" +
                "stockPriceHistoryId='" + stockPriceHistoryId + '\'' +
                ", stockId='" + stockId + '\'' +
                ", dateTime=" + dateTime +
                ", openPrice=" + openPrice +
                ", closePrice=" + closePrice +
                ", highPrice=" + highPrice +
                ", lowPrice=" + lowPrice +
                '}';
    }
}
