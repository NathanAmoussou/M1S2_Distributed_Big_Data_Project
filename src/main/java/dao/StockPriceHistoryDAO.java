package dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import model.StockPriceHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockPriceHistoryDAO implements GenericDAO<StockPriceHistory> {
    private final MongoCollection<Document> collection;


    public StockPriceHistoryDAO(MongoDatabase database) {
        this.collection = database.getCollection("stockPriceHistory");
    }

    @Override
    public StockPriceHistory findById(String id) {
        throw new UnsupportedOperationException("Should not be called use findByStockPriceHistoryTickerAndDateTime instead");
    }

    @Override
    public void deleteById(String id) {
        throw new UnsupportedOperationException("Should not be called use deleteByStockPriceHistoryTickerAndDateTime instead");
    }

    @Override
    public List<StockPriceHistory> findAll() {
       try {
              List<StockPriceHistory> stockPriceHistories = new ArrayList<>();
              for (Document doc : collection.find()) {
                stockPriceHistories.add(documentToStockPriceHistory(doc));
              }
              return stockPriceHistories;
         } catch (Exception e) {
              e.printStackTrace();
              return null;
       }
    }

    @Override
    public void save(StockPriceHistory stockPriceHistory) {
       try{
           Document doc = new Document();
           doc.append("stockPriceHistoryTicker", stockPriceHistory.getStockPriceHistoryTicker());
           doc.append("openPrice", stockPriceHistory.getOpenPrice().toString());
           doc.append("closePrice", stockPriceHistory.getClosePrice().toString());
           doc.append("highPrice", stockPriceHistory.getHighPrice().toString());
           doc.append("lowPrice", stockPriceHistory.getLowPrice().toString());
           doc.append("dateTime", stockPriceHistory.getDateTime());
           collection.insertOne(doc);
       } catch (Exception e) {
           e.printStackTrace();
       }
    }

    @Override
    public void update(StockPriceHistory stockPriceHistory) {
        try{
            Document doc = new Document("stockPriceHistoryTicker", stockPriceHistory.getStockPriceHistoryTicker())
                    .append("openPrice", stockPriceHistory.getOpenPrice().toString())
                    .append("closePrice", stockPriceHistory.getClosePrice().toString())
                    .append("highPrice", stockPriceHistory.getHighPrice().toString())
                    .append("lowPrice", stockPriceHistory.getLowPrice().toString())
                    .append("dateTime", stockPriceHistory.getDateTime());
            // update the document with the same stockPriceHistoryTicker and same dateTime
            collection.updateOne(new Document("stockPriceHistoryTicker", stockPriceHistory.getStockPriceHistoryTicker())
                    .append("dateTime", stockPriceHistory.getDateTime()), new Document("$set", doc));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public StockPriceHistory findByStockPriceHistoryTickerAndDateTime(String ticker, LocalDateTime dateTime) {
        try {
            Document doc = collection.find(new Document("stockPriceHistoryTicker", ticker)
                    .append("dateTime", dateTime)).first();
            return doc != null ? documentToStockPriceHistory(doc) : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void deleteByStockPriceHistoryTickerAndDateTime(String ticker, LocalDateTime dateTime) {
        try {
            collection.deleteOne(new Document("stockPriceHistoryTicker", ticker)
                    .append("dateTime", dateTime));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private StockPriceHistory documentToStockPriceHistory(Document doc) {
        return new StockPriceHistory(
                doc.getString("stockPriceHistoryTicker"),
                LocalDateTime.ofInstant(doc.getDate("dateTime").toInstant(), java.time.ZoneId.systemDefault()),
                new BigDecimal(doc.getString("openPrice")),
                new BigDecimal(doc.getString("closePrice")),
                new BigDecimal(doc.getString("highPrice")),
                new BigDecimal(doc.getString("lowPrice"))
        );
    }
}
