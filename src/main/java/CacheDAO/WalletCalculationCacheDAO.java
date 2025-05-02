package CacheDAO;

import Config.AppConfig;
import Utils.RedisCacheService;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.util.Optional;

public class WalletCalculationCacheDAO {

    private static final String WALLET_VALUE_KEY_PREFIX = "wallet:value:";
    private static final String WALLET_PL_GLOBAL_KEY_PREFIX = "wallet:pl:global:";
    private static final String WALLET_PL_STOCK_KEY_PREFIX = "wallet:pl:stock:";

    private String getValueKey(ObjectId walletId) {
        return WALLET_VALUE_KEY_PREFIX + walletId.toString();
    }

    private String getGlobalPLKey(ObjectId walletId) {
        return WALLET_PL_GLOBAL_KEY_PREFIX + walletId.toString();
    }

    private String getStockPLKey(ObjectId walletId, String stockTicker) {
        return WALLET_PL_STOCK_KEY_PREFIX + walletId.toString() + ":" + stockTicker;
    }

    public Optional<JSONObject> findWalletValue(ObjectId walletId) {
        String key = getValueKey(walletId);
        String cachedJson = RedisCacheService.getCache(key);
        if (cachedJson != null) {
            try {
                System.out.println("Cache HIT for wallet value: " + key);
                return Optional.of(new JSONObject(cachedJson));
            } catch (Exception e) {
                System.err.println("Error deserializing wallet value from cache (key " + key + "): " + e.getMessage());
                RedisCacheService.deleteCache(key);
            }
        }
        System.out.println("Cache MISS for wallet value: " + key);
        return Optional.empty();
    }

    public void saveWalletValue(ObjectId walletId, JSONObject value) {
        String key = getValueKey(walletId);
        try {
            RedisCacheService.setCache(key, value.toString(), AppConfig.CACHE_TTL_SHORT);
            System.out.println("Cache SAVED for wallet value: " + key);
        } catch (Exception e) {
            System.err.println("Error serializing wallet value for cache (key " + key + "): " + e.getMessage());
        }
    }

    public void invalidateWalletValue(ObjectId walletId) {
        String key = getValueKey(walletId);
        RedisCacheService.deleteCache(key);
        System.out.println("Cache INVALIDATED for wallet value: " + key);
    }

    public Optional<JSONObject> findGlobalPL(ObjectId walletId) {
        String key = getGlobalPLKey(walletId);
        String cachedJson = RedisCacheService.getCache(key);
        if (cachedJson != null) {
            try {
                System.out.println("Cache HIT for global P/L: " + key);
                return Optional.of(new JSONObject(cachedJson));
            } catch (Exception e) {
                System.err.println("Error deserializing global P/L from cache (key " + key + "): " + e.getMessage());
                RedisCacheService.deleteCache(key);
            }
        }
        System.out.println("Cache MISS for global P/L: " + key);
        return Optional.empty();
    }

    public void saveGlobalPL(ObjectId walletId, JSONObject value) {
        String key = getGlobalPLKey(walletId);
        try {
            RedisCacheService.setCache(key, value.toString(), AppConfig.CACHE_TTL_SHORT);
            System.out.println("Cache SAVED for global P/L: " + key);
        } catch (Exception e) {
            System.err.println("Error serializing global P/L for cache (key " + key + "): " + e.getMessage());
        }
    }

    public void invalidateGlobalPL(ObjectId walletId) {
        String key = getGlobalPLKey(walletId);
        RedisCacheService.deleteCache(key);
        System.out.println("Cache INVALIDATED for global P/L: " + key);
    }

    public Optional<JSONObject> findStockPL(ObjectId walletId, String stockTicker) {
        String key = getStockPLKey(walletId, stockTicker);
        String cachedJson = RedisCacheService.getCache(key);
        if (cachedJson != null) {
            try {
                System.out.println("Cache HIT for stock P/L: " + key);
                return Optional.of(new JSONObject(cachedJson));
            } catch (Exception e) {
                System.err.println("Error deserializing stock P/L from cache (key " + key + "): " + e.getMessage());
                RedisCacheService.deleteCache(key);
            }
        }
        System.out.println("Cache MISS for stock P/L: " + key);
        return Optional.empty();
    }

    public void saveStockPL(ObjectId walletId, String stockTicker, JSONObject value) {
        String key = getStockPLKey(walletId, stockTicker);
        try {
            RedisCacheService.setCache(key, value.toString(), AppConfig.CACHE_TTL_SHORT);
            System.out.println("Cache SAVED for stock P/L: " + key);
        } catch (Exception e) {
            System.err.println("Error serializing stock P/L for cache (key " + key + "): " + e.getMessage());
        }
    }

    public void invalidateStockPL(ObjectId walletId, String stockTicker) {
        String key = getStockPLKey(walletId, stockTicker);
        RedisCacheService.deleteCache(key);
        System.out.println("Cache INVALIDATED for stock P/L: " + key);
    }

    public void invalidateAllPLForWallet(ObjectId walletId) {
        invalidateGlobalPL(walletId);

        System.out.println("Cache INVALIDATED for global P/L for wallet: " + walletId);
    }

    public void invalidateAllCalculationsForWallet(ObjectId walletId) {
        invalidateWalletValue(walletId);
        invalidateAllPLForWallet(walletId);
    }
}
