package dao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

import model.Stock;

public class StockDAO implements GenericDAO<Stock> {
    private final MongoCollection<Document> collection;

    public StockDAO(MongoDatabase database) {
        this.collection = database.getCollection("stocks");
        this.collection.createIndex(new Document("stockTicker", 1), new IndexOptions().unique(true));
    }

    public Stock findByStockTicker(String stockTicker) {
        Document doc = collection.find(new Document("stockTicker", stockTicker)).first();
        return doc != null ? documentToStock(doc) : null;
    }

    private Stock documentToStock(Document doc) {
        System.out.println("doc: " + doc.toJson());

        Object lastPriceObj = doc.get("lastPrice");
        String lastPriceStr;
        if (lastPriceObj instanceof String) {
            lastPriceStr = (String) lastPriceObj;
        } else if (lastPriceObj instanceof org.bson.types.Decimal128) {
            lastPriceStr = ((org.bson.types.Decimal128) lastPriceObj).bigDecimalValue().toString();
        } else {
            lastPriceStr = lastPriceObj.toString();
        }
        System.out.println("lastPrice: " + lastPriceStr);

        return new Stock(
                doc.getString("stockName"),
                doc.getString("stockTicker"),
                doc.getString("market"),
                doc.getString("industry"),
                doc.getString("sector"),
                new BigDecimal(lastPriceStr),
                LocalDateTime.ofInstant(doc.getDate("lastUpdated").toInstant(), java.time.ZoneId.systemDefault()),
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

    @Override
    public void save(Stock stock) {
        Document doc = new Document()
                .append("stockName", stock.getStockName())
                .append("stockTicker", stock.getStockTicker())
                .append("market", stock.getMarket())
                .append("industry", stock.getIndustry())
                .append("sector", stock.getSector())
                .append("lastPrice", stock.getLastPrice())
                .append("lastUpdated", stock.getLastUpdated());

        collection.insertOne(doc);
    }

    @Override
    public void update(Stock stock) {
        Document doc = new Document()
                .append("stockName", stock.getStockName())
                .append("stockTicker", stock.getStockTicker())
                .append("market", stock.getMarket())
                .append("industry", stock.getIndustry())
                .append("sector", stock.getSector())
                .append("lastPrice", stock.getLastPrice())
                .append("lastUpdated", stock.getLastUpdated());

        collection.replaceOne(new Document("stockTicker", stock.getStockTicker()), doc);
    }

//    @Override
//    public void deleteById(String id) {
//        throw new UnsupportedOperationException("Use deleteByStockTicker instead.");
//    }

    public void deleteByStockTicker(String stockTicker) {
        collection.deleteOne(new Document("stockTicker", stockTicker));
    }
}
