package Utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.UnifiedJedis;

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

    public static void clearAll() {
        System.out.println("Attempting to clear Redis cache (FLUSHDB)...");
        try {
            UnifiedJedis jedis = RedisClient.getInstance().getJedis();
            // ca marche pas
           // String result = jedis.flushDB();
          //  System.out.println("Redis FLUSHDB command executed. Result: " + result); // Doit retourner "OK"
        } catch (Exception e) {
            System.err.println("REDIS ERROR - Failed to execute FLUSHDB on Redis: " + e.getMessage());
            throw new RuntimeException("Failed to clear Redis cache", e);
        }
    }

    // CORRIGE : Conversion des objets en JSON et vice versa est directement fait dans les classes model

}
