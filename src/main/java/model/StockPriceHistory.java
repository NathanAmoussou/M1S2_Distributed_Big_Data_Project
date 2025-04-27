package model;

import com.google.gson.JsonObject;
import org.json.JSONObject;

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
    private BigDecimal stockSplit;


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
            BigDecimal stockSplit
    ) {
        this.stockPriceHistoryTicker = stockPriceHistoryTicker;
        this.dateTime = dateTime;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
        this.dividend = dividend;
        this.stockSplit = stockSplit;
    }

    // Constructor for JSON
    public StockPriceHistory(JSONObject json) {
        this.stockPriceHistoryTicker = json.getString("stockPriceHistoryTicker");
        this.dateTime = LocalDateTime.parse(json.getString("dateTime"));
        this.openPrice = new BigDecimal(json.getString("openPrice"));
        this.closePrice = new BigDecimal(json.getString("closePrice"));
        this.highPrice = new BigDecimal(json.getString("highPrice"));
        this.lowPrice = new BigDecimal(json.getString("lowPrice"));
        this.volume = new BigDecimal(json.getString("volume"));
        this.dividend = new BigDecimal(json.getString("dividend"));
        this.stockSplit = new BigDecimal(json.getString("stockSplit"));
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
    public BigDecimal getStockSplit() {
        return stockSplit;
    }
    public void setStockSplit(BigDecimal stockSplit) {
        this.stockSplit = stockSplit;
    }


    @Override
    public String toString() {
        JSONObject json = this.toJson();
        return json.toString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("stockPriceHistoryTicker", stockPriceHistoryTicker);
        json.put("dateTime", dateTime.toString());
        json.put("openPrice", openPrice.toString());
        json.put("closePrice", closePrice.toString());
        json.put("highPrice", highPrice.toString());
        json.put("lowPrice", lowPrice.toString());
        json.put("volume", volume.toString());
        json.put("dividend", dividend.toString());
        json.put("stockSplit", stockSplit.toString());
        return json;
    }

    public static StockPriceHistory fromJson(JSONObject json) {
        return new StockPriceHistory(
                json.getString("stockPriceHistoryTicker"),
                LocalDateTime.parse(json.getString("dateTime")),
                new BigDecimal(json.getString("openPrice")),
                new BigDecimal(json.getString("closePrice")),
                new BigDecimal(json.getString("highPrice")),
                new BigDecimal(json.getString("lowPrice")),
                new BigDecimal(json.getString("volume")),
                new BigDecimal(json.getString("dividend")),
                new BigDecimal(json.getString("stockSplit"))
        );
    }
}
