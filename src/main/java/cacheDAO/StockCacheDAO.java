package cacheDAO;

import config.AppConfig;
import model.Stock;
import org.json.JSONArray;
import org.json.JSONObject;
import util.RedisCacheService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StockCacheDAO implements GenericCacheDAO<Stock> {

    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final String ALL_STOCKS_KEY = "stocks:all";
    private static final String ALL_TICKERS_KEY = "assets:tickers:all";

    private String getStockKey(String ticker) {
        return STOCK_KEY_PREFIX + ticker;
    }

    @Override
    public Optional<Stock> findByTicker(String ticker) {
        String key = getStockKey(ticker);
        String cachedStockJson = RedisCacheService.getCache(key);
        if (cachedStockJson != null) {
            try {
                JSONObject json = new JSONObject(cachedStockJson);
                return Optional.of(new Stock(json));
            } catch (Exception e) {
                System.err.println("Erreur de désérialisation du Stock depuis le cache pour la clé " + key + ": " + e.getMessage());
                RedisCacheService.deleteCache(key);
                return Optional.empty();
            }
        }
        return Optional.empty();    }

    @Override
    public Optional<List<Stock>> findAll() {
        String cachedStocksJson = RedisCacheService.getCache(ALL_STOCKS_KEY);
        if (cachedStocksJson != null) {
            try {
                JSONArray arr = new JSONArray(cachedStocksJson);                     // peut contenir des String ou des JSONObject

                List<Stock> stocks = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject stockJson = new JSONObject(arr.getString(i));
                    stocks.add(new Stock(stockJson));
                }
                return Optional.of(stocks);
            } catch (Exception e) {
                System.err.println("Erreur de désérialisation de la liste des Stocks depuis le cache : " + e.getMessage());
                RedisCacheService.deleteCache(ALL_STOCKS_KEY);
                return Optional.empty();
            }
        }
        return Optional.empty();    }

    @Override
    public Optional<List<String>> findAllTickers() {
        String cachedTickersJson = RedisCacheService.getCache(ALL_TICKERS_KEY);
        if (cachedTickersJson != null) {
            try {
                JSONArray arr = new JSONArray(cachedTickersJson);
                List<String> tickers = arr.toList().stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
                return Optional.of(tickers);
            } catch (Exception e) {
                System.err.println("Erreur de désérialisation de la liste des tickers depuis le cache : " + e.getMessage());
                RedisCacheService.deleteCache(ALL_TICKERS_KEY);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public void save(Stock stock, int ttlSeconds) {
        String key = getStockKey(stock.getStockTicker());
        try {
            String stockJson = stock.toJson().toString();
            RedisCacheService.setCache(key, stockJson, ttlSeconds);
        } catch (Exception e) {
            System.err.println("Erreur de sérialisation du Stock pour mise en cache (clé " + key + "): " + e.getMessage());
        }
    }

    @Override
    public void saveAll(List<Stock> stocks, int ttlSeconds) {
        try {
            JSONArray arr = new JSONArray();
            for (Stock s : stocks) {
                arr.put(s.toJson().toString());
            }
            RedisCacheService.setCache(ALL_STOCKS_KEY, arr.toString(), ttlSeconds);
        } catch (Exception e) {
            System.err.println("Erreur de sérialisation de la liste des Stocks pour mise en cache: " + e.getMessage());
        }
    }

    @Override
    public void saveAllTickers(List<String> tickers, int ttlSeconds) {
        try {
            JSONArray tickersArray = new JSONArray(tickers);
            RedisCacheService.setCache(ALL_TICKERS_KEY, tickersArray.toString(), ttlSeconds);
        } catch (Exception e) {
            System.err.println("Erreur de sérialisation de la liste des tickers pour mise en cache: " + e.getMessage());
        }
    }

    @Override
    public void delete(String ticker) {
        String key = getStockKey(ticker);
        RedisCacheService.deleteCache(key);
    }

    @Override
    public void invalidateAll() {
        RedisCacheService.deleteCache(ALL_STOCKS_KEY);
    }

    @Override
    public void invalidateAllTickers() {
        RedisCacheService.deleteCache(ALL_TICKERS_KEY);
    }
}
