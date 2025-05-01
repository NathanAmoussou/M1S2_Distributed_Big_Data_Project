package Services;

import CacheDAO.HoldingCacheDAO;
import DAO.StockDAO;
import DAO.TransactionDAO;
import Models.Stock;
import Models.Transaction;
import Models.Wallet;
import com.mongodb.client.MongoDatabase;
import Config.AppConfig;
import DAO.HoldingsDAO;
import Models.Holding;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import java.time.LocalDateTime;

public class HoldingService {

    private final HoldingsDAO holdingsDAO;
    private final HoldingCacheDAO holdingCacheDAO;
    private final TransactionDAO transactionDAO;
    private final StockDAO stockDAO;

    public HoldingService(MongoDatabase database) {
        this.holdingsDAO = new HoldingsDAO(database);
        this.holdingCacheDAO = new HoldingCacheDAO();
        this.transactionDAO = new TransactionDAO(database);
        this.stockDAO = new StockDAO(database);
    }

    /**
     * Retrieves all holdings for a specific wallet.
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