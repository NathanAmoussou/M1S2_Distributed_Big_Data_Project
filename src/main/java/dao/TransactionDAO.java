package dao;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionDAO implements GenericDAO<Transaction> {
    private final MongoCollection<Document> collection;
    public TransactionDAO(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public Transaction findById(String id) {
        Document doc = collection.find(new Document("_id", id)).first();
        return doc != null ? documentToTransaction(doc) : null;
    }

    private Transaction documentToTransaction(Document doc) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(doc.getString("_id"));
        transaction.setPriceAtTransaction(new BigDecimal(doc.getString("priceAtTransaction")));
        transaction.setTransactionStatusId(doc.getString("transactionStatusId"));
        transaction.setQuantity(doc.getInteger("quantity"));
        transaction.setTransactionTypesId(doc.getString("transactionTypesId"));
        transaction.setCreatedAt(LocalDateTime.ofInstant(doc.getDate("createdAt").toInstant(), java.time.ZoneId.systemDefault()));
        transaction.setUpdatedAt(LocalDateTime.ofInstant(doc.getDate("updatedAt").toInstant(), java.time.ZoneId.systemDefault()));
        transaction.setStockId(doc.getString("stockId"));
        transaction.setWalletId(doc.getString("walletId"));

        return transaction;
    }

    @Override
    public List<Transaction> findAll() {
       List<Transaction> transactions = new ArrayList<>();
        for (Document doc : collection.find()) {
            transactions.add(documentToTransaction(doc));
        }
        return transactions;
    }

    @Override
    public void save(Transaction transaction) {
        Document doc = new Document();
        doc.append("_id", transaction.getTransactionId());
        doc.append("priceAtTransaction", transaction.getPriceAtTransaction().toString());
        doc.append("transactionStatusId", transaction.getTransactionStatusId());
        doc.append("quantity", transaction.getQuantity());
        doc.append("transactionTypesId", transaction.getTransactionTypesId());
        doc.append("createdAt", transaction.getCreatedAt());
        doc.append("stockId", transaction.getStockId());
        doc.append("walletId", transaction.getWalletId());
        doc.append("updatedAt", transaction.getUpdatedAt());
        collection.insertOne(doc);
    }

    @Override
    public void update(Transaction transaction) {
        Document doc = new Document();
        doc.append("_id", transaction.getTransactionId());
        doc.append("priceAtTransaction", transaction.getPriceAtTransaction().toString());
        doc.append("transactionStatusId", transaction.getTransactionStatusId());
        doc.append("quantity", transaction.getQuantity());
        doc.append("transactionTypesId", transaction.getTransactionTypesId());
        doc.append("createdAt", transaction.getCreatedAt());
        doc.append("stockId", transaction.getStockId());
        doc.append("walletId", transaction.getWalletId());
        doc.append("updatedAt", transaction.getUpdatedAt());
        collection.updateOne(new Document("_id", transaction.getTransactionId()), new Document("$set", doc));

    }

    @Override
    public void deleteById(String id) {
        collection.deleteOne(new Document("_id", id));

    }

    public List<Transaction> findByWalletId(String walletId) {
        List<Transaction> result = new ArrayList<>();
        for (Document doc : collection.find(new Document("walletId", walletId))) {
            result.add(documentToTransaction(doc));
        }
        return result;
    }

}
