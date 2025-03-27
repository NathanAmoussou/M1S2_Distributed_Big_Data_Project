package model;

public class TransactionStatus {
    private String transactionStatusKey;
    private String transactionStatusValue;

    public TransactionStatus() {
    }

    public TransactionStatus(String transactionStatusKey, String transactionStatusValue) {
        this.transactionStatusKey = transactionStatusKey;
        this.transactionStatusValue = transactionStatusValue;
    }

    public String getTransactionStatusKey() {
        return transactionStatusKey;
    }

    public void setTransactionStatusKey(String transactionStatusKey) {
        this.transactionStatusKey = transactionStatusKey;
    }

    public String getTransactionStatusValue() {
        return transactionStatusValue;
    }

    public void setTransactionStatusValue(String transactionStatusValue) {
        this.transactionStatusValue = transactionStatusValue;
    }

    @Override
    public String toString() {
        return "TransactionStatus{" +
                "transactionStatusKey='" + transactionStatusKey + '\'' +
                ", transactionStatusValue='" + transactionStatusValue + '\'' +
                '}';
    }
}
