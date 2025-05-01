package cacheDAO;

import config.AppConfig;
import model.Holding;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import util.RedisCacheService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HoldingCacheDAO {
    private static final String HOLDINGS_WALLET_KEY_PREFIX = "holdings:wallet:";
    private static final String HOLDING_INDIVIDUAL_KEY_PREFIX = "holding:wallet:";
    private static final String HOLDING_INDIVIDUAL_KEY_SEPARATOR = ":stock:";

    private String getWalletHoldingsKey(ObjectId walletId) {
        if (walletId == null) return null;
        return HOLDINGS_WALLET_KEY_PREFIX + walletId.toString();
    }

    private String getIndividualHoldingKey(ObjectId walletId, String stockTicker) {
        if (walletId == null || stockTicker == null || stockTicker.isEmpty()) return null;
        return HOLDING_INDIVIDUAL_KEY_PREFIX + walletId.toString() + HOLDING_INDIVIDUAL_KEY_SEPARATOR + stockTicker;
    }


    public Optional<List<Holding>> findByWalletId(ObjectId walletId) {
        String key = getWalletHoldingsKey(walletId);
        if (key == null) return Optional.empty();

        String cachedJson = RedisCacheService.getCache(key);
        if (cachedJson != null) {
            try {
                JSONArray array = new JSONArray(cachedJson);
                List<Holding> holdings = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    holdings.add(new Holding(new JSONObject(array.getString(i))));
                }
                return Optional.of(holdings);
            } catch (Exception e) {
                System.err.println("Erreur désérialisation liste Holdings (clé " + key + "): " + e.getMessage());
                RedisCacheService.deleteCache(key);
            }
        }
        return Optional.empty();
    }

    public void saveByWalletId(ObjectId walletId, List<Holding> holdings, int ttlSeconds) {
        String key = getWalletHoldingsKey(walletId);
        if (key == null) return;

        try {
            JSONArray array = new JSONArray();
            for (Holding h : holdings) {
                array.put(h.toJson().toString());
            }
            RedisCacheService.setCache(key, array.toString(), ttlSeconds);
        } catch (Exception e) {
            System.err.println("Erreur sérialisation liste Holdings pour cache (clé " + key + "): " + e.getMessage());
        }
    }

    public void invalidateByWalletId(ObjectId walletId) {
        String key = getWalletHoldingsKey(walletId);
        if (key == null) return;
        RedisCacheService.deleteCache(key);
        System.out.println("Cache invalidé pour les holdings du wallet: " + walletId);
    }

    public Optional<Holding> findByWalletIdAndStockTicker(ObjectId walletId, String stockTicker) {
        String key = getIndividualHoldingKey(walletId, stockTicker);
        if (key == null) return Optional.empty();

        String cachedJson = RedisCacheService.getCache(key);
        if (cachedJson != null) {
            try {
                return Optional.of(new Holding(new JSONObject(cachedJson)));
            } catch (Exception e) {
                System.err.println("Erreur désérialisation Holding individuel (clé " + key + "): " + e.getMessage());
                RedisCacheService.deleteCache(key);
            }
        }
        return Optional.empty();
    }

    public void saveOrUpdateIndividual(Holding holding, int ttlSeconds) {
        String key = getIndividualHoldingKey(holding.getWalletId(), holding.getStockTicker());
        if (key == null) return;

        try {
            RedisCacheService.setCache(key, holding.toJson().toString(), ttlSeconds);
        } catch (Exception e) {
            System.err.println("Erreur sérialisation Holding individuel pour cache (clé " + key + "): " + e.getMessage());
        }
    }

    public void deleteIndividual(ObjectId walletId, String stockTicker) {
        String key = getIndividualHoldingKey(walletId, stockTicker);
        if (key == null) return;
        RedisCacheService.deleteCache(key);
    }
}
