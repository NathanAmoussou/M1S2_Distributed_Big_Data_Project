package DAO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.Decimal128; // Import Decimal128

import com.mongodb.client.ClientSession; // Import ClientSession
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import Models.Stock;

public class StockDAO implements GenericDAO<Stock> {
    private final MongoCollection<Document> collection;

    public StockDAO(MongoDatabase database) {
        this.collection = database.getCollection("stocks");
        // this.collection.createIndex(new Document("stockTicker", 1), new IndexOptions().unique(true));
    }

    // findByStockTicker method to find a stock by its ticker symbol without session
    public Stock findByStockTicker(String stockTicker) {
        // Call the session-aware version with null session
        return findByStockTicker(null, stockTicker);
    }

    // Session-aware version of findByStockTicker
    public Stock findByStockTicker(ClientSession session, String stockTicker) {
        Document filter = new Document("stockTicker", stockTicker);
        Document doc = (session != null)
                ? collection.find(session, filter).first() // Use session if provided
                : collection.find(filter).first();          // Otherwise, use normal find

        return doc != null ? documentToStock(doc) : null;
    }


    // Helper method to convert Document to Stock (handles Decimal128)
    private Stock documentToStock(Document doc) {
        // System.out.println("doc: " + doc.toJson());

        Object lastPriceObj = doc.get("lastPrice");
        String lastPriceStr;

        if (lastPriceObj == null) {
            lastPriceStr = "0.0"; // fefault if null
        } else if (lastPriceObj instanceof String) {
            lastPriceStr = (String) lastPriceObj;
        } else if (lastPriceObj instanceof Decimal128) { // Handle Decimal128
            lastPriceStr = ((Decimal128) lastPriceObj).bigDecimalValue().toString();
        } else if (lastPriceObj instanceof Number) { // Handle other numeric types
            lastPriceStr = lastPriceObj.toString();
        }
        else {
            System.err.println("Warning: Unexpected type for lastPrice in stock " + doc.getString("stockTicker") + ": " + lastPriceObj.getClass().getName());
            lastPriceStr = "0.0";
        }
        // System.out.println("lastPrice: " + lastPriceStr);

        // potential null Date
        java.util.Date lastUpdatedDate = doc.getDate("lastUpdated");
        LocalDateTime lastUpdated = (lastUpdatedDate != null)
                ? LocalDateTime.ofInstant(lastUpdatedDate.toInstant(), java.time.ZoneId.systemDefault())
                : null;

        return new Stock(
                doc.getString("stockName"),
                doc.getString("stockTicker"),
                doc.getString("market"),
                doc.getString("industry"),
                doc.getString("sector"),
                new BigDecimal(lastPriceStr),
                lastUpdated,
                doc.getString("country"),
                doc.getString("currency")
        );
    }


    @Override
    public Stock findById(String id) {
        throw new UnsupportedOperationException("Use findByStockTicker instead.");
    }

    @Override
    public void deleteById(String id) {
        throw new UnsupportedOperationException("Use deleteByStockTicker instead.");
    }

    @Override
    public List<Stock> findAll() {
        List<Stock> stocks = new ArrayList<>();
        for (Document doc : collection.find()) {
            stocks.add(documentToStock(doc));
        }
        return stocks;
    }

    // save method to save a stock without session
    @Override
    public void save(Stock stock) {
        save(null, stock); // Call session-aware version
    }

    // save method with session overload
    public void save(ClientSession session, Stock stock) {
        Document doc = stockToDocument(stock); // Use helper method
        if (session != null) {
            collection.insertOne(session, doc);
        } else {
            collection.insertOne(doc);
        }
    }

    // update method to update a stock without session
    @Override
    public void update(Stock stock) {
        update(null, stock); // Call session-aware version
    }

    // update method with session overload
    public void update(ClientSession session, Stock stock) {
        Document filter = new Document("stockTicker", stock.getStockTicker());
        Document updateDoc = new Document("$set", stockToDocument(stock));
        // remove _id from update as it should not be updated
        updateDoc.get("$set", Document.class).remove("_id");

        if (session != null) {
            collection.updateOne(session, filter, updateDoc);
        } else {
            collection.updateOne(filter, updateDoc);
        }
    }


    public void deleteByStockTicker(String stockTicker) {
        collection.deleteOne(new Document("stockTicker", stockTicker));
    }

    // helper method to convert Stock to Document
    private Document stockToDocument(Stock stock) {
        return new Document()
                .append("stockName", stock.getStockName())
                .append("stockTicker", stock.getStockTicker())
                .append("market", stock.getMarket())
                .append("industry", stock.getIndustry())
                .append("sector", stock.getSector())
                .append("lastPrice", stock.getLastPrice())
                .append("lastUpdated", stock.getLastUpdated())
                .append("country", stock.getCountry())
                .append("currency", stock.getCurrency());
    }
}