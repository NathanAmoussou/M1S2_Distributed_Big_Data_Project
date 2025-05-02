package CacheDAO;


import Models.Transaction;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import Utils.RedisCacheService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransactionCacheDAO {
    private static final String TRANSACTIONS_WALLET_KEY_PREFIX = "transactions:wallet:";

    private String getWalletTransactionsKey(ObjectId walletId) {
        if (walletId == null) return null;
        return TRANSACTIONS_WALLET_KEY_PREFIX + walletId.toString();
    }

    public Optional<List<Transaction>> findByWalletId(ObjectId walletId) {
        String key = getWalletTransactionsKey(walletId);
        if (key == null) return Optional.empty();

        String cachedJson = RedisCacheService.getCache(key);
        if (cachedJson != null) {
            try {
                JSONArray array = new JSONArray(cachedJson);
                List<Transaction> transactions = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject txJson = new JSONObject(array.getString(i));
                    transactions.add(new Transaction(txJson));
                }
                System.out.println("Cache HIT for transaction list: " + key);
                return Optional.of(transactions);
            } catch (Exception e) {
                System.err.println("Erreur désérialisation liste Transactions (clé " + key + "): " + e.getMessage());
                RedisCacheService.deleteCache(key);
            }
        }
        System.out.println("Cache MISS for transaction list: " + key);
        return Optional.empty();
    }

    public void saveByWalletId(ObjectId walletId, List<Transaction> transactions) {
        String key = getWalletTransactionsKey(walletId);
        if (key == null) return;

        try {
            JSONArray array = new JSONArray();
            if (transactions != null) {
                for (Transaction t : transactions) {
                    array.put(t.toJson().toString());
                }
            }
            RedisCacheService.setCache(key, array.toString(), Config.AppConfig.CACHE_TTL);
            System.out.println("Cache SAVED for transaction list: " + key + " (" + (transactions != null ? transactions.size() : 0) + " items)");
        } catch (Exception e) {
            System.err.println("Erreur sérialisation liste Transactions pour cache (clé " + key + "): " + e.getMessage());
        }
    }

    public void invalidateByWalletId(ObjectId walletId) {
        String key = getWalletTransactionsKey(walletId);
        if (key == null) return;
        RedisCacheService.deleteCache(key);
        System.out.println("Cache invalidé pour les transactions du wallet: " + walletId);
    }
}
