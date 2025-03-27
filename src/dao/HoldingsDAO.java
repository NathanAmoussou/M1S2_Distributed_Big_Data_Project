package src.dao;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import src.model.Holdings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HoldingsDAO implements GenericDAO<Holdings>{
    private final MongoCollection<Document> collection;

    public HoldingsDAO(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public Holdings findById(String id) {
        Document doc = collection.find(new Document("_id", id)).first();
        return doc != null ? documentToHoldings(doc) : null;

    }

    @Override
    public List<Holdings> findAll() {
        List<Holdings> result = new ArrayList<>();
        for (Document doc : collection.find()) {
            result.add(documentToHoldings(doc));
        }
        return result;
    }

    @Override
    public void save(Holdings holdings) {
        Document doc = new Document("_id", holdings.getHoldingsId())
                .append("stockId", holdings.getStockId())
                .append("quantity", holdings.getQuantity())
                .append("averagePurchasePrice", holdings.getAveragePurchasePrice().toString())
                .append("walletId", holdings.getWalletId());
        collection.insertOne(doc);

    }

    @Override
    public void update(Holdings holdings) {
        Document doc = new Document("stockId", holdings.getStockId())
                .append("quantity", holdings.getQuantity())
                .append("averagePurchasePrice", holdings.getAveragePurchasePrice().toString())
                .append("walletId", holdings.getWalletId());
        collection.updateOne(new Document("_id", holdings.getHoldingsId()), new Document("$set", doc));

    }

    @Override
    public void deleteById(String id) {
        collection.deleteOne(new Document("_id", id));

    }

    private Holdings documentToHoldings(Document doc) {
        Holdings holdings = new Holdings();
        holdings.setHoldingsId(doc.getString("_id"));
        holdings.setStockId(doc.getString("stockId"));
        holdings.setQuantity(doc.getInteger("quantity"));
        holdings.setAveragePurchasePrice(new BigDecimal(doc.getString("averagePurchasePrice")));
        holdings.setWalletId(doc.getString("walletId"));
        return holdings;

    }


}
