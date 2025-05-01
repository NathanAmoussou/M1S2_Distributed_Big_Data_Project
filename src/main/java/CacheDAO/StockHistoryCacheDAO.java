package CacheDAO;

import Models.StockPriceHistory;
import Utils.RedisCacheService;
import org.json.JSONArray;
import org.json.JSONObject;
import Config.AppConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StockHistoryCacheDAO {
    private static final String HISTORY_30D_KEY_PREFIX = "stock:history:30d:";

    private String getHistoryKey(String ticker) {
        if (ticker == null || ticker.isEmpty()) {
            return null;
        }
        return HISTORY_30D_KEY_PREFIX + ticker;
    }

    public Optional<List<StockPriceHistory>> findByTicker(String ticker) {
        String key = getHistoryKey(ticker);
        if (key == null) {
            return Optional.empty();
        }

        String cachedJson = RedisCacheService.getCache(key);
        if (cachedJson != null) {
            try {
                JSONArray array = new JSONArray(cachedJson);
                List<StockPriceHistory> historyList = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {

                    //TODO verif convert json
                    Object item = array.get(i);
                    JSONObject historyJson;
                    if (item instanceof String) {
                        historyJson = new JSONObject((String) item);
                    } else if (item instanceof JSONObject) {
                        historyJson = (JSONObject) item;
                    } else {
                        throw new ClassCastException("Unexpected type in JSON array for history cache: " + item.getClass().getName());
                    }
                    historyList.add(new StockPriceHistory(historyJson));
                }
                System.out.println("Cache HIT for 30d history: " + key);
                return Optional.of(historyList);
            } catch (Exception e) {
                System.err.println("Erreur de désérialisation de l'historique depuis le cache (clé " + key + "): " + e.getMessage());
                RedisCacheService.deleteCache(key);
                return Optional.empty();
            }
        }
        System.out.println("Cache MISS for 30d history: " + key);
        return Optional.empty();
    }
    public void save(String ticker, List<StockPriceHistory> history) {
        String key = getHistoryKey(ticker);
        if (key == null || history == null) {
            return;
        }

        try {
            JSONArray array = new JSONArray();
            for (StockPriceHistory sph : history) {
                array.put(sph.toJson());
            }
            RedisCacheService.setCache(key, array.toString(), AppConfig.CACHE_TTL_LONG);
            System.out.println("Cache SAVED for 30d history: " + key + " (" + history.size() + " items)");
        } catch (Exception e) {
            System.err.println("Erreur de sérialisation de l'historique pour mise en cache (clé " + key + "): " + e.getMessage());
        }
    }

    public void invalidate(String ticker) {
        String key = getHistoryKey(ticker);
        if (key != null) {
            RedisCacheService.deleteCache(key);
            System.out.println("Cache INVALIDATED for 30d history: " + key);
        }
    }
}
