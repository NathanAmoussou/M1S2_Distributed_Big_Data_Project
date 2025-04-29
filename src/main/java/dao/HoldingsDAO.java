package dao;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import model.Holding;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class HoldingsDAO implements GenericDAO<Holding>{
    private final MongoCollection<Document> collection;

    public HoldingsDAO(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public Holding findById(String id) {
        Document doc = collection.find(new Document("_id", id)).first();
        return doc != null ? documentToHoldings(doc) : null;

    }

    @Override
    public List<Holding> findAll() {
        List<Holding> result = new ArrayList<>();
        for (Document doc : collection.find()) {
            result.add(documentToHoldings(doc));
        }
        return result;
    }

    @Override
    public void save(Holding holding) {
        try{
            JSONObject json = holding.toJson();
            Document doc = new Document(json.toMap());
            collection.insertOne(doc);
        } catch (Exception e) {
            System.out.println("Error saving holding: " + e.getMessage());
        }
    }

    @Override
    public void update(Holding holding) {
        try {
            JSONObject json = holding.toJson();
            Document doc = new Document(json.toMap());
            collection.updateOne( new Document("_id", holding.getHoldingId()), new Document("$set", doc));
        } catch (Exception e) {
            System.out.println("Error updating holding: " + e.getMessage());
        }
    }

    @Override
    public void deleteById(String id) {
        try {
            collection.deleteOne(new Document("_id", new ObjectId(id)));
        } catch (Exception e) {
            System.out.println("Error deleting holding: " + e.getMessage());
        }

    }

    private Holding documentToHoldings(Document doc) {
        try {
            Holding holding = new Holding(new JSONObject(doc.toJson()));
            return holding;
        } catch (Exception e) {
            System.out.println("Error converting document to Holding: " + e.getMessage());
            return null;
        }
    }


    public List<Holding> findByWalletId(ObjectId walletId) {
        List<Holding> result = new ArrayList<>();
        for (Document doc : collection.find(new Document("walletId", walletId))) {
            result.add(documentToHoldings(doc));
        }
        return result;
    }

}
