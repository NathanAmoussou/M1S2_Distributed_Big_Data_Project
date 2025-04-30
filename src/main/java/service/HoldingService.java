package service;

import com.mongodb.client.MongoDatabase;
import dao.HoldingsDAO;
import dao.StockDAO;
import dao.TransactionDAO;
import dao.InvestorDAO;
import model.Holding;
import model.Stock;
import model.Transaction;
import model.Wallet;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class HoldingService {

    private final HoldingsDAO holdingsDAO;

    public HoldingService(MongoDatabase database) {
        this.holdingsDAO = new HoldingsDAO(database.getCollection("holdings"));
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


        // adding caching here around holdingsDAO.findByWalletId

        return holdingsDAO.findByWalletId(walletObjId);
    }


}