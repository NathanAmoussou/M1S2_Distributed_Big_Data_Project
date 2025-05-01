package Services;

import CacheDAO.HoldingCacheDAO;
import com.mongodb.client.MongoDatabase;
import Config.AppConfig;
import DAO.HoldingsDAO;
import Models.Holding;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

public class HoldingService {

    private final HoldingsDAO holdingsDAO;
    private final HoldingCacheDAO holdingCacheDAO;

    public HoldingService(MongoDatabase database) {
        this.holdingsDAO = new HoldingsDAO(database.getCollection("holdings"));
        this.holdingCacheDAO = new HoldingCacheDAO();
    }

    /**
     * Retrieves all holdings for a specific wallet.
     * @param walletIdStr The String representation of the wallet's ObjectId.
     * @return A list of Holding objects, or an empty list if none found.
     * @throws IllegalArgumentException if walletIdStr is not a valid ObjectId format.
     */
    public List<Holding> getHoldingsByWalletId(String walletIdStr) throws IllegalArgumentException {

        if (!ObjectId.isValid(walletIdStr)) {
            throw new IllegalArgumentException("Invalid walletId format: " + walletIdStr);
        }
        ObjectId walletObjId = new ObjectId(walletIdStr);

        if (AppConfig.isEnabled()){
            Optional<List<Holding>> cachedHoldings = holdingCacheDAO.findByWalletId(walletObjId);
            if (cachedHoldings.isPresent()) {
                System.out.println("Holdings for wallet [ " + walletIdStr +  "] found in cache.");
                return cachedHoldings.get();
            }
        }
        List<Holding> holdings = holdingsDAO.findByWalletId(walletObjId);

        if (AppConfig.isEnabled() && holdings != null) {
            holdingCacheDAO.saveByWalletId(walletObjId, holdings, AppConfig.CACHE_TTL);
        }
        // adding caching here around holdingsDAO.findByWalletId

        return holdings;
    }


}