package dao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import model.StockPriceHistory;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;

public class StockPriceHistoryDAO implements GenericDAO<StockPriceHistory> {
    private final MongoCollection<Document> collection;


    public StockPriceHistoryDAO(MongoDatabase database) {
        this.collection = database.getCollection("stockPriceHistory");
        this.collection.createIndex(new Document("stockPriceHistoryTicker", 1)); // we ensure the tickers are indexed
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

    /**
     * Find all price history records for a given stock ticker
     * @param ticker The stock ticker to find history for
     * @return List of StockPriceHistory objects
     */
    public List<StockPriceHistory> findAllByTicker(String ticker) {
        try {
            List<StockPriceHistory> stockPriceHistories = new ArrayList<>();
            for (Document doc : collection.find(new Document("stockPriceHistoryTicker", ticker))) {
                stockPriceHistories.add(documentToStockPriceHistory(doc));
            }
            return stockPriceHistories;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public void save(StockPriceHistory stockPriceHistory) {
        try {
            Document doc = new Document();
            doc.append("stockPriceHistoryTicker", stockPriceHistory.getStockPriceHistoryTicker());
            doc.append("openPrice", stockPriceHistory.getOpenPrice());
            doc.append("closePrice", stockPriceHistory.getClosePrice());
            doc.append("highPrice", stockPriceHistory.getHighPrice());
            doc.append("lowPrice", stockPriceHistory.getLowPrice());
            doc.append("volume", stockPriceHistory.getVolume());
            doc.append("dividend", stockPriceHistory.getDividend());
            doc.append("stockSplit", stockPriceHistory.getStockSplit());
            doc.append("dateTime", stockPriceHistory.getDateTime());
            collection.insertOne(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(StockPriceHistory stockPriceHistory) {
        try {
            Document doc = new Document("stockPriceHistoryTicker", stockPriceHistory.getStockPriceHistoryTicker())
                    .append("openPrice", stockPriceHistory.getOpenPrice())
                    .append("closePrice", stockPriceHistory.getClosePrice())
                    .append("highPrice", stockPriceHistory.getHighPrice())
                    .append("lowPrice", stockPriceHistory.getLowPrice())
                    .append("volume", stockPriceHistory.getVolume())
                    .append("dividend", stockPriceHistory.getDividend())
                    .append("stockSplit", stockPriceHistory.getStockSplit())
                    .append("dateTime", stockPriceHistory.getDateTime());
            
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
        StockPriceHistory history = new StockPriceHistory();
        
        history.setStockPriceHistoryTicker(doc.getString("stockPriceHistoryTicker"));
        history.setDateTime(LocalDateTime.ofInstant(doc.getDate("dateTime").toInstant(), 
                            java.time.ZoneId.systemDefault()));
        
        history.setOpenPrice(safeGetBigDecimal(doc, "openPrice"));
        history.setClosePrice(safeGetBigDecimal(doc, "closePrice"));
        history.setHighPrice(safeGetBigDecimal(doc, "highPrice"));
        history.setLowPrice(safeGetBigDecimal(doc, "lowPrice"));
        history.setVolume(safeGetBigDecimal(doc, "volume"));
        history.setDividend(safeGetBigDecimal(doc, "dividend"));
        history.setStockSplit(safeGetBigDecimal(doc, "stockSplit"));
        
        return history;
    }

    private BigDecimal safeGetBigDecimal(Document doc, String fieldName) {
        Object value = doc.get(fieldName);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof Decimal128) {
            return ((Decimal128) value).bigDecimalValue();
        } else if (value instanceof Number) {
            return new BigDecimal(((Number) value).toString());
        } else if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    /// /////////////////////

    // Add these methods
    public List<StockPriceHistory> findByTickerAndDateRangePaginated(String ticker, LocalDateTime startDateTime, LocalDateTime endDateTime, int skip, int limit) {
        List<StockPriceHistory> results = new ArrayList<>();
        try {
            // Build the filter
            List<Bson> filters = new ArrayList<>();
            filters.add(Filters.eq("stockPriceHistoryTicker", ticker));
            if (startDateTime != null) {
                filters.add(Filters.gte("dateTime", startDateTime));
            }
            if (endDateTime != null) {
                filters.add(Filters.lte("dateTime", endDateTime));
            }
            Bson finalFilter = filters.isEmpty() ? new Document() : Filters.and(filters);

            collection.find(finalFilter)
                    .sort(Sorts.ascending("dateTime"))
                    .skip(skip)
                    .limit(limit)
                    .forEach(doc -> results.add(documentToStockPriceHistory(doc)));
            return results;
        } catch (Exception e) {
            System.err.println("Error finding paginated stock history: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public long countByTickerAndDateRange(String ticker, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        try {
            // Build the filter (same as above)
            List<Bson> filters = new ArrayList<>();
            filters.add(Filters.eq("stockPriceHistoryTicker", ticker));
            if (startDateTime != null) {
                filters.add(Filters.gte("dateTime", startDateTime));
            }
            if (endDateTime != null) {
                filters.add(Filters.lte("dateTime", endDateTime));
            }
            Bson finalFilter = filters.isEmpty() ? new Document() : Filters.and(filters);

            return collection.countDocuments(finalFilter);
        } catch (Exception e) {
            System.err.println("Error counting stock history: " + e.getMessage());
            e.printStackTrace();
            return 0; // Return 0 on error
        }
    }


}
