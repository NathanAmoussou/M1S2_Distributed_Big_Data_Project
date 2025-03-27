package dao;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import model.TransactionTypes;

import java.util.ArrayList;
import java.util.List;

public class TransactionTypesDAO implements GenericDAO<TransactionTypes>{
    private final MongoCollection<Document> collection;

    public TransactionTypesDAO(MongoCollection<Document> collection) {
        this.collection = collection;
    }


    @Override
    public TransactionTypes findById(String id) {
        Document doc = collection.find(new Document("_id", id)).first();
        return doc != null ? documentToTransactionTypes(doc) : null;
    }

    private TransactionTypes documentToTransactionTypes(Document doc) {
        TransactionTypes transactionTypes = new TransactionTypes();
        transactionTypes.setTransactionTypesKey(doc.getString("_id"));
        transactionTypes.setTransactionTypesValue(doc.getString("transactionTypesValue"));
        return transactionTypes;
    }

    @Override
    public List<TransactionTypes> findAll() {
        List<TransactionTypes> result = new ArrayList<>();
        for (Document doc : collection.find()) {
            result.add(documentToTransactionTypes(doc));
        }
        return result;
    }

    @Override
    public void save(TransactionTypes transactionTypes) {
        Document doc = new Document("_id", transactionTypes.getTransactionTypesKey())
                .append("transactionTypesValue", transactionTypes.getTransactionTypesValue());
        collection.insertOne(doc);
    }

    @Override
    public void update(TransactionTypes transactionTypes) {
        Document doc = new Document("transactionTypesValue", transactionTypes.getTransactionTypesValue());
        collection.updateOne(new Document("_id", transactionTypes.getTransactionTypesKey()), new Document("$set", doc));
    }

    @Override
    public void deleteById(String id) {
        collection.deleteOne(new Document("_id", id));
    }
}
