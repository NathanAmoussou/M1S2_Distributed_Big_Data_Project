package dao;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import model.Transaction;
import org.bson.types.ObjectId;
import org.json.JSONObject;

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
        try{
            Transaction transaction = new Transaction(new JSONObject(doc.toJson()));
            return transaction;
        } catch (Exception e) {
            System.out.println("Error converting document to Transaction: " + e.getMessage());
            return null;
        }

    }

    @Override
    public List<Transaction> findAll() { // WE SHOULD NOT USE FIND ALL AS WE ARE IN BIG DATA AND A LOT OF DOCS
       List<Transaction> transactions = new ArrayList<>();
        for (Document doc : collection.find()) {
            transactions.add(documentToTransaction(doc));
        }
        return transactions;
    }

    @Override
    public void save(Transaction transaction) {
        try {
            JSONObject json = transaction.toJson();
            Document doc = new Document(json.toMap());
            collection.insertOne(doc);
        } catch (Exception e) {
            System.out.println("Error saving transaction: " + e.getMessage());
        }
    }

    @Override
    public void update(Transaction transaction) {
        try {
            JSONObject json = transaction.toJson();
            Document doc = new Document(json.toMap());
            System.out.println("Updating transaction: " + transaction);
            collection.updateOne(new Document("_id", transaction.getTransactionId()), new Document("$set", doc));
        } catch (Exception e) {
            System.out.println("Error updating transaction: " + e.getMessage());
        }


    }

    @Override
    public void deleteById(String id) {
        try {
            collection.deleteOne(new Document("_id", new ObjectId(id)));
        } catch (Exception e) {
            System.out.println("Error deleting transaction: " + e.getMessage());
        }
    }

    public List<Transaction> getTransactionsByWalletId(ObjectId walletId) {
        List<Transaction> result = new ArrayList<>();
        for (Document doc : collection.find(new Document("walletId", walletId))) {
            result.add(documentToTransaction(doc));
        }
        return result;
    }

    public List<Transaction> getTransactionsByStockId(String stockTicker) {
        List<Transaction> result = new ArrayList<>();
        for (Document doc : collection.find(new Document("stockId", stockTicker))) {
            result.add(documentToTransaction(doc));
        }
        return result;
    }

}
