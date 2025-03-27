package src.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import src.model.Stock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockDAO implements GenericDAO<Stock> {
    private final MongoCollection<Document> collection;
    public StockDAO(MongoDatabase database) {
        this.collection = database.getCollection("stocks");
    }

    @Override
    public Stock findById(String id) {
        Document doc = collection.find(new Document("_id", id)).first();
        return doc != null ? documentToStock(doc) : null;
    }

    private Stock documentToStock(Document doc) {
        Stock stock = new Stock();
        stock.setStockId(doc.getString("_id"));
        stock.setStockName(doc.getString("stockName"));
        stock.setStockTicker(doc.getString("stockTicker"));
        stock.setLastPrice(new BigDecimal(doc.getString("lastPrice")));
        stock.setMarket(doc.getString("market"));
        stock.setSector(doc.getString("sector"));
        stock.setLastUpdated(LocalDateTime.from(doc.getDate("lastUpdated").toInstant()));

        return stock;
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
        Document doc = new Document();
        doc.append("_id", stock.getStockId());
        doc.append("stockName", stock.getStockName());
        doc.append("stockTicker", stock.getStockTicker());
        doc.append("lastPrice", stock.getLastPrice().toString());
        doc.append("market", stock.getMarket());
        doc.append("sector", stock.getSector());
        doc.append("lastUpdated", stock.getLastUpdated());
        collection.insertOne(doc);
    }

    @Override
    public void update(Stock stock) {
        Document doc = new Document();
        doc.append("_id", stock.getStockId());
        doc.append("stockName", stock.getStockName());
        doc.append("stockTicker", stock.getStockTicker());
        doc.append("lastPrice", stock.getLastPrice().toString());
        doc.append("market", stock.getMarket());
        doc.append("sector", stock.getSector());
        doc.append("lastUpdated", stock.getLastUpdated());
        collection.replaceOne(new Document("_id", stock.getStockId()), doc);
    }

    @Override
    public void deleteById(String id) {
        collection.deleteOne(new Document("_id", id));
    }
}
