package model;

import com.google.gson.JsonObject;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import util.JsonUtils;

import java.math.BigDecimal;

public class Wallet {
    private ObjectId walletId;
    private String currencyCode;
    private BigDecimal balance; //wesh c est int infini du jamais vu //
    private String walletTypeId;

    public Wallet() {
        this.setWalletId(new ObjectId());
        this.setCurrencyCode("USD");
        this.setBalance(BigDecimal.ZERO);
        this.setWalletTypeId("default");
    }

    public Wallet(ObjectId walletId, String currencyCode, BigDecimal balance, String walletTypeId) {
        this.walletId = walletId; //.toHexString();
        this.currencyCode = currencyCode;
        this.balance = balance;
        this.walletTypeId = walletTypeId;
    }

    // Constructor for JSON
    public Wallet(JSONObject json) {
        this.walletId = JsonUtils.getObjectId(json, "walletId");
        this.currencyCode = json.getString("currencyCode");
        this.walletTypeId = json.getString("walletTypeId");
        this.balance = JsonUtils.getBigDecimal(json, "balance");
    }

    public ObjectId getWalletId() {
        return walletId;
    }

    public void setWalletId(ObjectId walletId) {
        this.walletId = walletId;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getWalletTypeId() {
        return walletTypeId;
    }

    public void setWalletTypeId(String walletTypeId) {
        this.walletTypeId = walletTypeId;
    }

    @Override
    public String toString() {
        JSONObject json = this.toJson();
        return json.toString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("walletId", walletId);
        json.put("walletType", walletTypeId);
        json.put("currencyCode", currencyCode);
        json.put("balance", balance);
        return json;
    }

}
