package util;

import model.Investor;
import model.Stock;
import model.Transaction;
import model.Wallet;
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

    // CORRIGE : Conversion des objets en JSON et vice versa est directement fait dans les classes model

}
