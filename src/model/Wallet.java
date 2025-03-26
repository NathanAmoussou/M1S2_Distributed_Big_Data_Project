package src.model;

import java.math.BigDecimal;
import java.util.List;

public class Wallet {
    private String walletId;
    private String currencyCode;
    private BigDecimal balance; //wesh c est int infini du jamais vu

    private Investor investor;

    private WalletType walletType;

    public Wallet() {
    }

    public Wallet(String walletId, String currencyCode, BigDecimal balance, Investor investor, WalletType walletType) {
        this.walletId = walletId;
        this.currencyCode = currencyCode;
        this.balance = balance;
        this.investor = investor;
        this.walletType = walletType;
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

    public Investor getInvestor() {
        return investor;
    }

    public void setInvestor(Investor investor) {
        this.investor = investor;
    }

    public WalletType getWalletType() {
        return walletType;
    }

    public void setWalletType(WalletType walletType) {
        this.walletType = walletType;
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "walletId='" + walletId + '\'' +
                ", currencyCode='" + currencyCode + '\'' +
                ", balance=" + balance +
                ", investor=" + investor +
                ", walletType=" + walletType +
                '}';
    }
}
