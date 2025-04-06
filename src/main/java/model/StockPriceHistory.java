package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockPriceHistory {
    private String stockPriceHistoryTicker; // Must include the market ex: "MC.PA"
    private LocalDateTime dateTime;
    private BigDecimal openPrice;
    private BigDecimal closePrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal volume;
    private BigDecimal dividend;
    private BigDecimal stock_split;


    public StockPriceHistory() {
    }

    public StockPriceHistory(
            String stockPriceHistoryTicker,
            LocalDateTime dateTime,
            BigDecimal openPrice,
            BigDecimal closePrice,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            BigDecimal volume,
            BigDecimal dividend,
            BigDecimal stock_split
    ) {
        this.stockPriceHistoryTicker = stockPriceHistoryTicker;
        this.dateTime = dateTime;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
        this.dividend = dividend;
        this.stock_split = stock_split;
    }

    public String getStockPriceHistoryTicker() {
        return stockPriceHistoryTicker;
    }

    public void setStockPriceHistoryTicker(String stockPriceHistoryTicker) {
        this.stockPriceHistoryTicker = stockPriceHistoryTicker;
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

    public BigDecimal getVolume() {
        return volume;
    }
    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }
    public BigDecimal getDividend() {
        return dividend;
    }
    public void setDividend(BigDecimal dividend) {
        this.dividend = dividend;
    }
    public BigDecimal getStock_split() {
        return stock_split;
    }
    public void setStock_split(BigDecimal stock_split) {
        this.stock_split = stock_split;
    }


    @Override
    public String toString() {
        return "StockPriceHistory{" +
                "stockPriceHistoryTicker='" + stockPriceHistoryTicker + '\'' +
                ", dateTime=" + dateTime +
                ", openPrice=" + openPrice +
                ", closePrice=" + closePrice +
                ", highPrice=" + highPrice +
                ", lowPrice=" + lowPrice +
                ", volume=" + volume +
                ", dividend=" + dividend +
                ", stock_split=" + stock_split +
                '}';
    }
}
