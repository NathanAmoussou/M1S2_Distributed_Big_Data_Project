package Models;

import org.bson.types.ObjectId;
import org.json.JSONObject;
import Utils.JsonUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Holding {
    private ObjectId holdingId; // "_id" of mongodb
    private ObjectId walletId;
    private String stockTicker;
    private BigDecimal quantity;
    private BigDecimal totalBuyCost;
    private BigDecimal totalSellCost;
    private LocalDateTime lastUpdated;

    public Holding() {
    }

    public Holding(String holdingsId, ObjectId walletId, String stockTicker, BigDecimal quantity, BigDecimal totalBuyCost, BigDecimal totalSellCost, LocalDateTime lastUpdated) {
        this.holdingId = new ObjectId(holdingsId);
        this.walletId = walletId;
        this.stockTicker = stockTicker;
        this.quantity = quantity;
        this.totalBuyCost = totalBuyCost;
        this.totalSellCost = totalSellCost;
        this.lastUpdated = lastUpdated;
    }

    //json constructor
    public Holding(JSONObject json) {
        this.holdingId = JsonUtils.getObjectId(json, "_id");
        this.walletId = JsonUtils.getObjectId(json, "walletId");
        this.stockTicker = json.getString("stockTicker");
        this.quantity = JsonUtils.getBigDecimal(json, "quantity");
        if (json.has("totalBuyCost")) {
            this.totalBuyCost = JsonUtils.getBigDecimal(json, "totalBuyCost");
        } else {
            this.totalBuyCost = BigDecimal.ZERO;
        }
        if (json.has("totalSellCost")) {
            this.totalSellCost = JsonUtils.getBigDecimal(json, "totalSellCost");
        } else {
            this.totalSellCost = BigDecimal.ZERO;
        }
        Object lastUpdateDateObj = json.opt("lastUpdateDate");
        this.lastUpdated =  (lastUpdateDateObj instanceof LocalDateTime) ?
                (LocalDateTime) lastUpdateDateObj :
                (lastUpdateDateObj instanceof String) ?
                        LocalDateTime.parse((String) lastUpdateDateObj) :
                        LocalDateTime.now();

    }

    public ObjectId getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(ObjectId holdingId) {
        this.holdingId = holdingId;
    }

    public ObjectId getWalletId() {
        return walletId;
    }

    public void setWalletId(ObjectId walletId) {
        this.walletId = walletId;
    }

    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalBuyCost() {
        return totalBuyCost;
    }
    public void setTotalBuyCost(BigDecimal totalBuyCost) {
        this.totalBuyCost = totalBuyCost;
    }
    public BigDecimal getTotalSellCost() {
        return totalSellCost;
    }
    public void setTotalSellCost(BigDecimal totalSellCost) {
        this.totalSellCost = totalSellCost;
    }
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        JSONObject json = this.toJson();
        return json.toString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("_id", holdingId);
        json.put("walletId", walletId);
        json.put("stockTicker", stockTicker);
        json.put("quantity", quantity);
        json.put("totalBuyCost", totalBuyCost);
        json.put("totalSellCost", totalSellCost);
        json.put("lastUpdated", lastUpdated);
        return json;
    }
}
