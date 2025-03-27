package model;

import java.math.BigDecimal;
import java.util.List;

public class Wallet {
    private String walletId;
    private String currencyCode;
    private BigDecimal balance; //wesh c est int infini du jamais vu

    private String investorId;

    private String walletTypeId;

    public Wallet() {
    }

    public Wallet(String walletId, String currencyCode, BigDecimal balance, String investorId, String walletTypeId) {
        this.walletId = walletId;
        this.currencyCode = currencyCode;
        this.balance = balance;
        this.investorId = investorId;
        this.walletTypeId = walletTypeId;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
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

    public String getInvestorId() {
        return investorId;
    }

    public void setInvestorId(String investorId) {
        this.investorId = investorId;
    }

    public String getWalletTypeId() {
        return walletTypeId;
    }

    public void setWalletTypeId(String walletTypeId) {
        this.walletTypeId = walletTypeId;
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "walletId='" + walletId + '\'' +
                ", currencyCode='" + currencyCode + '\'' +
                ", balance=" + balance +
                ", investorId='" + investorId + '\'' +
                ", walletTypeId='" + walletTypeId + '\'' +
                '}';
    }
}
