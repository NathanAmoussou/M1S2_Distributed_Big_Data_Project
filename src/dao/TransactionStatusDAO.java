package src.dao;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import src.model.TransactionStatus;

import java.util.ArrayList;
import java.util.List;

public class TransactionStatusDAO implements GenericDAO<TransactionStatus> {
    private final MongoCollection<Document> collection;
    public TransactionStatusDAO(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public TransactionStatus findById(String id) {
        Document doc = collection.find(new Document("_id", id)).first();
        return doc != null ? documentToTransactionStatus(doc) : null;
    }

    private TransactionStatus documentToTransactionStatus(Document doc) {
        TransactionStatus transactionStatus = new TransactionStatus();
        transactionStatus.setTransactionStatusKey(doc.getString("_id"));
        transactionStatus.setTransactionStatusValue(doc.getString("transactionStatusValue"));

        return transactionStatus;
    }

    @Override
    public List<TransactionStatus> findAll() {
        List<TransactionStatus> result = new ArrayList<>();
        for (Document doc : collection.find()) {
            result.add(documentToTransactionStatus(doc));
        }
        return result;
    }

    @Override
    public void save(TransactionStatus transactionStatus) {
        Document doc = new Document("_id", transactionStatus.getTransactionStatusKey())
                .append("transactionStatusValue", transactionStatus.getTransactionStatusValue());
        collection.insertOne(doc);
    }

    @Override
    public void update(TransactionStatus transactionStatus) {
        Document doc = new Document("transactionStatusValue", transactionStatus.getTransactionStatusValue());
        collection.updateOne(new Document("_id", transactionStatus.getTransactionStatusKey()), new Document("$set", doc));
    }

    @Override
    public void deleteById(String id) {
        collection.deleteOne(new Document("_id", id));
    }
}
