package src.dao;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import src.model.StockPriceHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockPriceHistoryDAO implements GenericDAO<StockPriceHistory>{
    private MongoCollection<Document> collection;
    public StockPriceHistoryDAO(MongoCollection<Document> collection) {
        this.collection = collection;
    }


    @Override
    public StockPriceHistory findById(String id) {
        Document doc = collection.find(new Document("_id", id)).first();
        return doc != null ? documentToStockPriceHistory(doc) : null;
    }

    private StockPriceHistory documentToStockPriceHistory(Document doc) {
        StockPriceHistory stockPriceHistory = new StockPriceHistory();
        stockPriceHistory.setStockId(doc.getString("_id"));
        stockPriceHistory.setClosePrice(new BigDecimal(doc.getString("closePrice")));
        stockPriceHistory.setHighPrice(new BigDecimal(doc.getString("highPrice")));
        stockPriceHistory.setLowPrice(new BigDecimal(doc.getString("lowPrice")));
        stockPriceHistory.setOpenPrice(new BigDecimal(doc.getString("openPrice")));
        stockPriceHistory.setDateTime(LocalDateTime.from(doc.getDate("dateTime").toInstant()));
        stockPriceHistory.setStockPriceHistoryId(doc.getString("stockPriceHistoryId"));
        return stockPriceHistory;
    }

    @Override
    public List<StockPriceHistory> findAll() {
        List<StockPriceHistory> stockPriceHistories = new ArrayList<>();
        for (Document doc : collection.find()) {
            stockPriceHistories.add(documentToStockPriceHistory(doc));
        }
        return stockPriceHistories;
    }

    @Override
    public void save(StockPriceHistory stockPriceHistory) {
        Document doc = new Document();
        doc.append("_id", stockPriceHistory.getStockId());
        doc.append("stockPriceHistoryId", stockPriceHistory.getStockPriceHistoryId());
        doc.append("openPrice", stockPriceHistory.getOpenPrice().toString());
        doc.append("closePrice", stockPriceHistory.getClosePrice().toString());
        doc.append("highPrice", stockPriceHistory.getHighPrice().toString());
        doc.append("lowPrice", stockPriceHistory.getLowPrice().toString());
        doc.append("dateTime", stockPriceHistory.getDateTime());
        collection.insertOne(doc);
    }

    @Override
    public void update(StockPriceHistory stockPriceHistory) {
        Document doc = new Document();
        doc.append("_id", stockPriceHistory.getStockId());
        doc.append("stockPriceHistoryId", stockPriceHistory.getStockPriceHistoryId());
        doc.append("openPrice", stockPriceHistory.getOpenPrice().toString());
        doc.append("closePrice", stockPriceHistory.getClosePrice().toString());
        doc.append("highPrice", stockPriceHistory.getHighPrice().toString());
        doc.append("lowPrice", stockPriceHistory.getLowPrice().toString());
        doc.append("dateTime", stockPriceHistory.getDateTime());
        collection.replaceOne(new Document("_id", stockPriceHistory.getStockId()), doc);

    }

    @Override
    public void deleteById(String id) {
        collection.deleteOne(new Document("_id", id));

    }
}
