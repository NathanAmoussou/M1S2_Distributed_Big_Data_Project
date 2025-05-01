package Services;

import CacheDAO.StockHistoryCacheDAO;
import DAO.StockPriceHistoryDAO;
import Models.StockPriceHistory;
import com.mongodb.client.MongoDatabase;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class StockHistoryService {

    private final StockPriceHistoryDAO historyDao;
    private final StockHistoryCacheDAO historyCacheDAO;

    public StockHistoryService(MongoDatabase database) {
        this.historyDao = new StockPriceHistoryDAO(database);
        this.historyCacheDAO = new StockHistoryCacheDAO();
    }

    public JSONObject getStockPercentageChange(String ticker, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        StockPriceHistory firstRecord = historyDao.findFirstRecordInRange(ticker, startDateTime, endDateTime);
        StockPriceHistory lastRecord = historyDao.findLastRecordInRange(ticker, startDateTime, endDateTime);

        JSONObject result = new JSONObject();
        result.put("ticker", ticker);
        result.put("startDate", startDateTime != null ? startDateTime.toString() : "N/A");
        result.put("endDate", endDateTime != null ? endDateTime.toString() : "N/A");

        if (firstRecord == null || lastRecord == null) {
            result.put("error", "Could not find sufficient historical data for the given range.");
            result.put("startPrice", "N/A");
            result.put("endPrice", "N/A");
            result.put("percentageChange", "N/A");
            return result;
        }

        // Use close price for calculation? Or open? Be consistent. Let's use close.
        BigDecimal startPrice = firstRecord.getClosePrice();
        BigDecimal endPrice = lastRecord.getClosePrice();

        result.put("startRecordDate", firstRecord.getDateTime().toString());
        result.put("endRecordDate", lastRecord.getDateTime().toString());
        result.put("startPrice", startPrice);
        result.put("endPrice", endPrice);

        if (startPrice.compareTo(BigDecimal.ZERO) == 0) {
            result.put("percentageChange", "N/A (start price is zero)");
        } else {
            try {
                // Calculate: ((end - start) / start) * 100
                BigDecimal change = endPrice.subtract(startPrice);
                // Use MathContext for controlled division precision
                java.math.MathContext mc = new java.math.MathContext(4, java.math.RoundingMode.HALF_UP); // 4 significant digits
                BigDecimal percentageChange = change.divide(startPrice, mc).multiply(new BigDecimal("100"));
                result.put("percentageChange", percentageChange);
            } catch (ArithmeticException e) {
                result.put("percentageChange", "N/A (calculation error)");
                System.err.println("Arithmetic error calculating percentage change for " + ticker + ": " + e.getMessage());
            }
        }
        return result;
    }

    public List<StockPriceHistory> getHistoryLast30Days(String ticker) {
        if (ticker == null || ticker.isEmpty()) {
            return Collections.emptyList();
        }

        if (Config.AppConfig.isEnabled()) {
            Optional<List<StockPriceHistory>> cachedHistory = historyCacheDAO.findByTicker(ticker);
            if (cachedHistory.isPresent()) {
                return cachedHistory.get();
            }
        }

        List<StockPriceHistory> dbHistory = historyDao.findLastNDaysByTicker(ticker, 30);

        if (dbHistory != null && !dbHistory.isEmpty() && Config.AppConfig.isEnabled()) {
            historyCacheDAO.save(ticker, dbHistory);
        }

        return dbHistory == null ? Collections.emptyList() : dbHistory;
    }
}
