package DAO;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import Models.Holding;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HoldingsDAO implements GenericDAO<Holding>{
    private final MongoCollection<Document> collection;
    private final MongoCollection<Document> stocksCollection;

    public HoldingsDAO(MongoDatabase database) {
        this.collection = database.getCollection("holdings");
        this.stocksCollection = database.getCollection("stocks");
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

    // save method without session
    @Override
    public void save(Holding holding) {
        save(null, holding);
    }


    // save method with session
    public void save(ClientSession session, Holding holding) {
        if (holding == null) {
            throw new IllegalArgumentException("Holding to save cannot be null");
        }
        try{
            JSONObject json = holding.toJson();
            Document doc = new Document(json.toMap());
            if (session != null) {
                collection.insertOne(session, doc);
            } else {
                collection.insertOne(doc);
            }
        } catch (Exception e) {
//            System.out.println("Error saving holding: " + e.getMessage());
            throw new RuntimeException("Error saving holding: " + e.getMessage());
        }
    }

    // update method without session
    @Override
    public void update(Holding holding) {
        update(null, holding); // Call session-aware version
    }

    // update method with session
    public void update(ClientSession session, Holding holding) {
        if (holding == null || holding.getHoldingId() == null) {
            throw new IllegalArgumentException("Holding and its ID cannot be null for update");
        }
        try {
            JSONObject json = holding.toJson();
            Document doc = new Document(json.toMap());
            Document filter = new Document("_id", holding.getHoldingId());
            Document updateDoc = new Document("$set", doc);
            // Remove _id from $set part
            updateDoc.get("$set", Document.class).remove("_id");
            if (session != null) {
                collection.updateOne(session, filter, updateDoc);
            } else {
                collection.updateOne(filter, updateDoc);
            }
        } catch (Exception e) {
//            System.out.println("Error updating holding: " + e.getMessage());
            throw new RuntimeException("Error updating holding: " + e.getMessage());
        }
    }

    @Override
    public void deleteById(String id) {
        try {
            collection.deleteOne(new Document("_id", new ObjectId(id)));
        } catch (Exception e) {
//            System.out.println("Error deleting holding: " + e.getMessage());
            throw new RuntimeException("Error deleting holding: " + e.getMessage());
        }

    }

    private Holding documentToHoldings(Document doc) {
        try {
            return new Holding(new JSONObject(doc.toJson()));
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

    public List<Holding> findByStockTicker(String stockTicker) {
        List<Holding> result = new ArrayList<>();
        for (Document doc : collection.find(new Document("stockTicker", stockTicker))) {
            result.add(documentToHoldings(doc));
        }
        return result;
    }

    // findByWalletIdAndStockTicker method without session
    public Holding findByWalletIdAndStockTicker(ObjectId walletId, String stockTicker) {
        return findByWalletIdAndStockTicker(null, walletId, stockTicker);
    }

    // findByWalletIdAndStockTicker method with session
    public Holding findByWalletIdAndStockTicker(ClientSession session, ObjectId walletId, String stockTicker) {
        Document filter = new Document("walletId", walletId).append("stockTicker", stockTicker);
        Document doc = (session != null)
                ? collection.find(session, filter).first()
                : collection.find(filter).first();
//        System.out.println("debug findByWalletIdAndStockTicker (session: " + (session!=null) + ") : " + doc);
        return doc != null ? documentToHoldings(doc) : null;
    }


    public boolean hasHoldingsForWallets(List<ObjectId> walletIds) {
        if (walletIds == null || walletIds.isEmpty()) {
            return false;
        }
        // Check if any document exists where walletId is in the provided list of walletIds
        Document filter = new Document("walletId", new Document("$in", walletIds));
        return collection.countDocuments(filter) > 0;
    }


    public BigDecimal getTotalHoldingsValueByAggregation(ObjectId walletId) {
        if (stocksCollection == null) {
            System.err.println("Cannot calculate holdings value: stocksCollection is null in HoldingsDAO.");
            return BigDecimal.ZERO; // Or throw an exception
        }

        List<Bson> pipeline = Arrays.asList(
                // match holdings for the specific wallet
                Aggregates.match(Filters.eq("walletId", walletId)),

                // lookup current price from the stocks collection
                Aggregates.lookup(
                        "stocks",            // Foreign collection
                        "stockTicker",       // Local field in holdings
                        "stockTicker",       // Foreign field in stocks
                        "stockInfo"          // Output array field name
                ),

                // unwind the stockInfo array (should only be one match per ticker)
                Aggregates.unwind("$stockInfo"),

                // project to calculate the value of each holding (quantity * currentPrice)
                //    Handle potential Decimal128 type for prices in stocks collection
                Aggregates.project(Projections.fields(
                        Projections.computed("holdingValue",
                                new Document("$multiply", Arrays.asList("$quantity", "$stockInfo.lastPrice"))
                        )
                )),

                // group to sum up the value of all holdings for the wallet
                Aggregates.group(
                        "$walletId", // group by walletId maybe null since one wallet ?
                        Accumulators.sum("totalValue", "$holdingValue") // Sum the calculated holdingValue
                )
        );

        try {
            // execute the aggregation pipeline
            Document result = collection.aggregate(pipeline).first();

            if (result != null && result.get("totalValue") != null) {
                try {
                    return new BigDecimal(result.get("totalValue").toString());
                } catch (NumberFormatException e) {
                    System.err.println("Error converting totalValue to BigDecimal: " + e.getMessage());
                    return BigDecimal.ZERO;
                }
            } else {
                return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            System.err.println("Error calculating total holdings value via aggregation for wallet " + walletId + ": " + e.getMessage());
            e.printStackTrace();
            return BigDecimal.ZERO; // Return zero on error
        }
    }
}

