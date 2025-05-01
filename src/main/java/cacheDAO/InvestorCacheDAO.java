package cacheDAO;

import config.AppConfig;
import model.Wallet;
import org.bson.types.ObjectId; // Importer ObjectId
import model.Investor;
import org.json.JSONObject;
import util.RedisCacheService;

import java.util.Optional;

public class InvestorCacheDAO {
    private static final String INVESTOR_KEY_PREFIX = "investor:";


    private String getInvestorKey(String investorId) {
        if (investorId == null || !ObjectId.isValid(investorId)) {
            System.err.println("Tentative de génération de clé cache avec un investorId invalide: " + investorId);
            return null;
        }
        return INVESTOR_KEY_PREFIX + investorId;
    }

    public Optional<Investor> findById(String investorId) {
        String key = getInvestorKey(investorId);
        if (key == null) return Optional.empty();

        String cachedJson = RedisCacheService.getCache(key);
        if (cachedJson != null) {
            try {
                return Optional.of(new Investor(new JSONObject(cachedJson)));
            } catch (Exception e) {
                System.err.println("Erreur désérialisation Investor (clé " + key + "): " + e.getMessage());
                RedisCacheService.deleteCache(key);
            }
        }
        return Optional.empty();
    }

    public void save(Investor investor, int ttlSeconds) {
        String key = getInvestorKey(investor.getInvestorId().toString());
        if (key == null) return;

        try {
            RedisCacheService.setCache(key, investor.toJson().toString(), ttlSeconds);
        } catch (Exception e) {
            System.err.println("Erreur sérialisation Investor pour cache (clé " + key + "): " + e.getMessage());
        }
    }

    public void delete(String investorId) {
        String key = getInvestorKey(investorId);
        if (key == null) return;
        RedisCacheService.deleteCache(key);
    }
}
