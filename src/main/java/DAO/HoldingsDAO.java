package DAO;

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

    public Holding findByWalletIdAndStockTicker(ObjectId walletId, String stockTicker) {
        Document doc = collection.find(new Document("walletId", walletId).append("stockTicker", stockTicker)).first();
        System.out.println("debug findByWalletIdAndStockTicker : " + doc);
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

