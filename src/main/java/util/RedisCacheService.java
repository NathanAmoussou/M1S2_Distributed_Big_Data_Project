package util;

import model.Stock;
import org.json.JSONObject;
import redis.clients.jedis.UnifiedJedis;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RedisCacheService {
    private static final UnifiedJedis jedis = RedisClient.getInstance().getJedis();

    /**
     * Sauvegarde une valeur dans le cache avec expiration
     * @param key La clé
     * @param value La valeur (en String, typiquement du JSON)
     * @param expirationInSeconds Durée d'expiration en secondes
     */
    public static void setCache(String key, String value, int expirationInSeconds) {
        jedis.setex(key, expirationInSeconds, value);
    }

    /**
     * Récupère la valeur en cache pour une clé donnée
     * @param key La clé du cache
     * @return La valeur enregistrée ou null
     */
    public static String getCache(String key) {
        return jedis.get(key);
    }

    /**
     * Supprime la clé du cache
     * @param key La clé à supprimer
     */
    public static void deleteCache(String key) {
        jedis.del(key);
    }

    public static JSONObject stockToJson(Stock stock) {
        JSONObject json = new JSONObject();
        json.put("stockName", stock.getStockName());
        json.put("stockTicker", stock.getStockTicker());
        json.put("market", stock.getMarket());
        json.put("industry", stock.getIndustry());
        json.put("sector", stock.getSector());
        json.put("lastPrice", stock.getLastPrice().toString());
        //json.put("lastUpdated", stock.getLastUpdated().toString()); //TODO pas sur
        json.put("lastUpdated", stock.getLastUpdated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        json.put("country", stock.getCountry());
        json.put("currency", stock.getCurrency());
        return json;
    }

    // Méthode pour reconstituer un objet Stock à partir d'un JSONObject
    public static Stock jsonToStock(JSONObject json) {
        return new Stock(
                json.getString("stockName"),
                json.getString("stockTicker"),
                json.getString("market"),
                json.getString("industry"),
                json.getString("sector"),
                new BigDecimal(json.getString("lastPrice")),
                //LocalDateTime.parse(json.getString("lastUpdated")), //TODO pas sur
                LocalDateTime.parse(json.getString("lastUpdated"), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                json.getString("country"),
                json.getString("currency")
        );
    }
}
