package Services;

import CacheDAO.InvestorCacheDAO;
import CacheDAO.WalletCalculationCacheDAO;
import DAO.HoldingsDAO;
import DAO.StockDAO;
import DAO.TransactionDAO;
import Models.*;
import com.mongodb.client.MongoDatabase;
import Config.AppConfig;
import DAO.InvestorDAO;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class InvestorService {
    private final InvestorDAO investorDAO;
    private final MongoDatabase database;
    private final InvestorCacheDAO investorCacheDAO;
    private final HoldingsDAO holdingsDAO;
    private final StockDAO stockDAO;
    private final TransactionDAO transactionDAO;
    private final WalletCalculationCacheDAO walletCalcCacheDAO;

    public InvestorService(MongoDatabase database) {
        this.database = database;
        this.investorDAO = new InvestorDAO(database);
        this.investorCacheDAO = new InvestorCacheDAO();
        this.holdingsDAO = new HoldingsDAO(database);
        this.stockDAO = new StockDAO(database);
        this.transactionDAO = new TransactionDAO(database);
        this.walletCalcCacheDAO = new WalletCalculationCacheDAO();
    }

    /**
     * Crée un nouvel investisseur et son portefeuille associé.
     */
    public Investor createInvestor(Investor investor) {
        // Ensure that the investor has at least one address
        if (investor.getAddresses() == null || investor.getAddresses().isEmpty()) {
            throw new IllegalArgumentException("The investor must have at least one Address.");
        }

        // Set creation and last update dates
        investor.setCreationDate(LocalDateTime.now());
        investor.setLastUpdateDate(LocalDateTime.now());

        // Set the default wallet if not present
        if (investor.getWallets() == null || investor.getWallets().isEmpty()) {
            Wallet wallet = new Wallet();
            investor.setWallets(List.of(wallet));
        }

        // save the investor (MongoDB will generate the _id automatically)
        investorDAO.save(investor);

        // Cache the investor if enabled
        if (AppConfig.isEnabled() && investor.getInvestorId() != null) {
            investorCacheDAO.save(investor, AppConfig.CACHE_TTL);
            System.out.println("Investor created: " + investor+ " and has been cached.");
        }

        return investor;
    }

    public Investor getInvestor(String investorId) {
        try {
            if (AppConfig.isEnabled()) {
                Optional<Investor> investor = investorCacheDAO.findById(investorId);
                if (investor.isPresent()) {
                    System.out.println("Investor found: " + investor);
                    return investor.get();
                }
                System.out.println("Investor not found in the cache: " + investor);
            }

            Investor investor = investorDAO.findById(investorId);

            if (investor != null && AppConfig.isEnabled()) {
                investorCacheDAO.save(investor, AppConfig.CACHE_TTL);
                System.out.println("Investor has been cached.");
            }
            return investor;
        } catch(Exception e) {
            System.err.println("Erreur lors de la lecture de l'investisseur: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Investor getInvestorByEmail(String email) {

        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format provided.");
        }

        Investor investor = investorDAO.findByEmail(email);
        if (investor != null) {
            System.out.println("Investor found by email [" + email + "].");
        } else {
            System.out.println("Investor not found for email [" + email + "].");
        }
        return investor;
    }

    public Investor getInvestorByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }

        Investor investor = investorDAO.findByUsername(username);
        if (investor != null) {
            System.out.println("Investor found by username [" + username + "].");
        } else {
            System.out.println("Investor not found for username [" + username + "].");
        }
        return investor;
    }

    public List<Investor> getAllInvestors() {
        return investorDAO.findAll();
    }

    /**
     * Ajoute des fonds au portefeuille d’un investisseur.
     */
    public Wallet addFundsToWallet(String walletId, BigDecimal amount) {
        Investor investor = investorDAO.findInvestorByWalletId(walletId);
        if (investor == null) {
            throw new RuntimeException("Aucun investisseur trouvé pour walletId: " + walletId);
        }
        System.out.println("Investisseur trouvé pour walletId : " + investor);

        Wallet wallet = investor.getWallets().stream()
                .filter(w -> w.getWalletId().toString().equals(walletId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Aucun portefeuille trouvé pour walletId: " + walletId + " dans l'investisseur: " + investor));

        wallet.setBalance(wallet.getBalance().add(amount));

        // Save the updated investor (MongoDB will update the document with the new wallet balance)
        this.updateInvestor(investor);

        if (AppConfig.isEnabled()) {
            walletCalcCacheDAO.invalidateAllCalculationsForWallet(wallet.getWalletId());
        }

        return wallet;
    }

    public Investor updateInvestor(Investor investor) {
        investor.setLastUpdateDate(LocalDateTime.now());

        // Ensure that the investor has at least one address before updating
        if (investor.getAddresses() == null || investor.getAddresses().isEmpty()) {
            throw new IllegalArgumentException("L'investisseur doit avoir au moins une adresse.");
        }

        // Update the investor with embedded addresses and wallets
        investorDAO.update(investor);

        // Cache the updated investor if enabled
        if (AppConfig.isEnabled()) {
            investorCacheDAO.save(investor, AppConfig.CACHE_TTL);
        }

        return investor;
    }

    public Wallet getWalletById(String walletId) {
        Wallet wallet = investorDAO.getWalletById(walletId);
        if (wallet == null) {
            throw new RuntimeException("Aucun portefeuille trouvé pour walletId: " + walletId);
        }
        return wallet;
    }

    public Wallet createWalletForInvestor(String investorId, Wallet newWalletData) {
        Investor investor = getInvestor(investorId); // Use existing method (handles validation, cache)
        if (investor == null) {
            throw new RuntimeException("Investor not found with ID: " + investorId); // Or specific exception
        }

        // Create the new Wallet object
        Wallet newWallet = new Wallet();
        newWallet.setWalletId(new ObjectId());
        newWallet.setCurrencyCode(newWalletData.getCurrencyCode() != null ? newWalletData.getCurrencyCode() : "USD"); // Default currency
        newWallet.setWalletType(newWalletData.getWalletType() != null ? newWalletData.getWalletType() : "default"); // Default type
        newWallet.setBalance(BigDecimal.ZERO); // new wallets do not have a balance

        // Add to investor's list
        investor.getWallets().add(newWallet);
        investor.setLastUpdateDate(LocalDateTime.now());

        // Save changes and handle cache via updateInvestor
        updateInvestor(investor);

        return newWallet; // Return the newly created wallet object
    }

    public Investor addAddressToInvestor(String investorId, Address newAddress) {
        if (newAddress == null) {
            throw new IllegalArgumentException("Address cannot be null.");
        }
        Investor investor = getInvestor(investorId); // Uses existing method with validation & caching
        if (investor == null) {
            throw new RuntimeException("Investor not found with ID: " + investorId); // Or specific exception
        }

        // Ensure the new address has an ID
        if (newAddress.getAddressId() == null) {
            newAddress.setAddressId(new ObjectId());
        }

        investor.getAddresses().add(newAddress);
        investor.setLastUpdateDate(LocalDateTime.now());

        // Use the existing updateInvestor which handles DAO and cache
        return updateInvestor(investor);
    }

    public Investor updateInvestorAddress(String investorId, String addressId, Address updatedAddress) {
        if (!ObjectId.isValid(addressId)) {
            throw new IllegalArgumentException("Invalid Address ID format: " + addressId);
        }
        if (updatedAddress == null || updatedAddress.getAddressId() == null) {
            throw new IllegalArgumentException("Updated address data and its ID cannot be null.");
        }
        if (!updatedAddress.getAddressId().toString().equals(addressId)) {
            throw new IllegalArgumentException("Address ID in path (" + addressId + ") does not match Address ID in body ("+ updatedAddress.getAddressId() +").");
        }

        Investor investor = getInvestor(investorId); // Uses existing method with validation & caching
        if (investor == null) {
            throw new RuntimeException("Investor not found with ID: " + investorId);
        }

        ObjectId targetAddressId = new ObjectId(addressId);
        boolean found = false;
        for (int i = 0; i < investor.getAddresses().size(); i++) {
            Address currentAddress = investor.getAddresses().get(i);
            if (currentAddress.getAddressId() != null && currentAddress.getAddressId().equals(targetAddressId)) {
                // Replace the existing address with the updated one
                investor.getAddresses().set(i, updatedAddress);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Address not found with ID: " + addressId + " for investor: " + investorId);
        }

        investor.setLastUpdateDate(LocalDateTime.now());

        // Use the existing updateInvestor which handles DAO and cache
        return updateInvestor(investor);
    }

    public Investor removeAddressFromInvestor(String investorId, String addressId) {
        if (!ObjectId.isValid(addressId)) {
            throw new IllegalArgumentException("Invalid Address ID format: " + addressId);
        }
        Investor investor = getInvestor(investorId);
        if (investor == null) {
            throw new RuntimeException("Investor not found with ID: " + investorId);
        }

        // Constraint: Cannot remove the last address
        if (investor.getAddresses().size() <= 1) {
            throw new IllegalArgumentException("Cannot remove the last address of an investor.");
        }

        ObjectId targetAddressId = new ObjectId(addressId);
        AtomicBoolean removed = new AtomicBoolean(false);

        // Use removeIf for cleaner removal
        investor.getAddresses().removeIf(address -> {
            if (address.getAddressId() != null && address.getAddressId().equals(targetAddressId)) {
                removed.set(true);
                return true; // Remove this element
            }
            return false; // Keep this element
        });


        if (!removed.get()) {
            throw new RuntimeException("Address not found with ID: " + addressId + " for investor: " + investorId); // More specific exception?
        }

        investor.setLastUpdateDate(LocalDateTime.now());

        // Use the existing updateInvestor which handles DAO and cache
        return updateInvestor(investor);
    }

    public Investor updateInvestorDetails(String investorId, Map<String, Object> fieldsToUpdate) {
        if (fieldsToUpdate == null || fieldsToUpdate.isEmpty()) {
            throw new IllegalArgumentException("No fields provided for update.");
        }

        // Fetch investor first to ensure it exists (optional, DAO update can check matchedCount)
        // Investor existingInvestor = getInvestor(investorId); // Use if you need pre-check
        // if (existingInvestor == null) {
        //    throw new RuntimeException("Investor not found: " + investorId);
        // }

        Document updateDoc = new Document();
        // Only allow specific fields to be updated via this method
        List<String> allowedFields = List.of("username", "password", "name", "surname", "email", "phoneNumber");

        for (Map.Entry<String, Object> entry : fieldsToUpdate.entrySet()) {
            if (allowedFields.contains(entry.getKey()) && entry.getValue() != null) { // Check if allowed and not null
                updateDoc.put(entry.getKey(), entry.getValue());
            }
        }

        if (updateDoc.isEmpty()) {
            throw new IllegalArgumentException("No valid or allowed fields provided for update.");
        }

        boolean success = investorDAO.updateInvestorPartial(investorId, updateDoc);

        if (success) {
            // TODO : CHECK IF THE CACHE THING WORKS ?
            // Invalidate cache
            if (AppConfig.isEnabled()) {
                investorCacheDAO.delete(investorId);
                System.out.println("Cache invalidated for updated investor: " + investorId);

                // refresh cache
                Investor updatedInvestor = getInvestor(investorId);
                if (updatedInvestor != null) {
                    investorCacheDAO.save(updatedInvestor, AppConfig.CACHE_TTL);
                    System.out.println("Updated investor cached: " + updatedInvestor);
                }

            }

            // Return the updated investor by fetching it again
            return getInvestor(investorId); // Fetch fresh data after update
        } else {
            // This could mean investor not found, or no fields actually changed.
            // The DAO method logs 'not found'. Here we might return null or throw.
            System.err.println("Update operation did not succeed for investor " + investorId + ". May not exist or no changes applied.");
            throw new RuntimeException("Failed to update investor details for ID: " + investorId + ". Investor might not exist.");
        }
    }

    // DELETE INVESTOR and check if the investor has any holdings in any of their wallets before
    public boolean deleteInvestor(String investorId) {
        Investor investor = getInvestor(investorId); // Ensures investor exists first
        if (investor == null) {
            throw new RuntimeException("Investor not found, cannot delete: " + investorId);
        }

        // --- Constraint Check ---
        // Check if the investor has any holdings in any of their wallets
        // since we haven't implemented away to withdraw balance it is not possible to delete an investor...
        List<ObjectId> walletIds = investor.getWallets().stream().map(Wallet::getWalletId).collect(Collectors.toList());
        if (holdingsDAO.hasHoldingsForWallets(walletIds)) {
            throw new RuntimeException("Cannot delete investor " + investorId + ": Investor still has holdings in one or more wallets - investor must sell all its action before.");
        }
        // Add check for non-zero balance?
        boolean hasBalance = investor.getWallets().stream().anyMatch(w -> w.getBalance().compareTo(BigDecimal.ZERO) > 0);
        if (hasBalance) {
            throw new RuntimeException("Cannot delete investor " + investorId + ": One or more wallets still have a positive balance - investor must withdraw all its money before.");
        }

        // --- Perform Deletion ---
        try {
            investorDAO.deleteById(investorId); // Use the correct DAO method
            System.out.println("Deleted investor: " + investorId);

            // Invalidate cache
            if (AppConfig.isEnabled()) {
                investorCacheDAO.delete(investorId);
                System.out.println("Cache invalidated for deleted investor: " + investorId);
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error during investor deletion in DAO for ID " + investorId + ": " + e.getMessage());
            e.printStackTrace();
            // Depending on DAO exception, maybe throw specific error
            throw new RuntimeException("Database error during investor deletion: " + e.getMessage());
        }
    }

    public JSONObject getWalletCurrentValue(String walletIdStr) {
        Wallet wallet = getWalletById(walletIdStr); // Handles validation and not found via exception

        if (AppConfig.isEnabled()) {
            Optional<JSONObject> cachedValue = walletCalcCacheDAO.findWalletValue(wallet.getWalletId());
            if (cachedValue.isPresent()) {
                System.out.println("Wallet " + walletIdStr + " cached: " + cachedValue);
                return cachedValue.get();
            }
        }

        BigDecimal cashBalance = wallet.getBalance();
        BigDecimal holdingsValue = BigDecimal.ZERO;

        List<Holding> holdings = holdingsDAO.findByWalletId(wallet.getWalletId()); // Fetch holdings

        for (Holding holding : holdings) {
            if (holding.getQuantity().compareTo(BigDecimal.ZERO) > 0) { // Only consider holdings with quantity
                Stock stock = stockDAO.findByStockTicker(holding.getStockTicker()); // Fetch current stock info (use cache if possible)
                if (stock != null) {
                    BigDecimal currentPrice = stock.getLastPrice();
                    BigDecimal holdingValue = holding.getQuantity().multiply(currentPrice);
                    holdingsValue = holdingsValue.add(holdingValue);
                } else {
                    // Handle case where stock data is missing - log warning, skip?
                    System.err.println("Warning: Could not find stock data for ticker " + holding.getStockTicker() + " while calculating wallet value.");
                }
            }
        }

        BigDecimal totalValue = cashBalance.add(holdingsValue);

        JSONObject result = new JSONObject();
        result.put("walletId", wallet.getWalletId().toString());
        result.put("currency", wallet.getCurrencyCode());
        result.put("cashBalance", cashBalance);
        result.put("holdingsValue", holdingsValue);
        result.put("totalValue", totalValue);
        result.put("calculationTimestamp", LocalDateTime.now().toString());

        if (AppConfig.isEnabled()) {
            walletCalcCacheDAO.saveWalletValue(wallet.getWalletId(), result);
        }

        return result;
    }


    /// /////////////////////////////////////////////////////
    ///
    /// //TODO: WE SHOULD MOVE THIS TO SEPARATE SERVICE HERE MAYBE NOT THE BEST PLACE SINCE IT RELIES ON MANY COLLEC

    public JSONObject getWalletStockProfitLoss(String walletIdStr, String stockTicker) {
        if (!ObjectId.isValid(walletIdStr)) {
            throw new IllegalArgumentException("Invalid Wallet ID format: " + walletIdStr);
        }
        ObjectId walletId = new ObjectId(walletIdStr);

        if (AppConfig.isEnabled()) {
            Optional<JSONObject> cachedPL = walletCalcCacheDAO.findStockPL(walletId, stockTicker);
            if (cachedPL.isPresent()) {
                System.out.println("Wallet " + walletIdStr + " cached: " + cachedPL);
                return cachedPL.get();
            }
        }

        // Get Current Holding
        Holding currentHolding = holdingsDAO.findByWalletIdAndStockTicker(walletId, stockTicker);
        BigDecimal currentQuantity = (currentHolding != null) ? currentHolding.getQuantity() : BigDecimal.ZERO;

        // Get Current Stock Price
        Stock stock = stockDAO.findByStockTicker(stockTicker);
        if (stock == null) {
            throw new RuntimeException("Stock data not found for ticker: " + stockTicker);
        }
        BigDecimal currentPrice = stock.getLastPrice();

        // Calculate Current Value of Holding
        BigDecimal currentValue = currentQuantity.multiply(currentPrice);

        // Get All Transactions for this Wallet/Stock
        List<Transaction> transactions = transactionDAO.findTransactionsByWalletAndStock(walletId, stockTicker);

        // Calculate Total Spent (Buys) and Total Received (Sells)
        BigDecimal totalMoneySpent = BigDecimal.ZERO;
        BigDecimal totalMoneyReceived = BigDecimal.ZERO;

        for (Transaction tx : transactions) {
            BigDecimal transactionValue = tx.getQuantity().multiply(tx.getPriceAtTransaction());
            if ("BUY".equalsIgnoreCase(tx.getTransactionTypesId())) { // Assuming "BUY" type
                totalMoneySpent = totalMoneySpent.add(transactionValue);
            } else if ("SELL".equalsIgnoreCase(tx.getTransactionTypesId())) { // Assuming "SELL" type
                totalMoneyReceived = totalMoneyReceived.add(transactionValue);
            }
            // we ignore other transaction
        }

        // Calculate Profit/Loss
        // P/L = (Current Value + Money Received from Sells) - Money Spent on Buys
        BigDecimal profitLoss = currentValue.add(totalMoneyReceived).subtract(totalMoneySpent);

        // Build Response
        JSONObject result = new JSONObject();
        result.put("walletId", walletIdStr);
        result.put("stockTicker", stockTicker);
        result.put("currentQuantity", currentQuantity);
        result.put("currentPrice", currentPrice);
        result.put("currentValue", currentValue);
        result.put("totalSpentOnBuys", totalMoneySpent);
        result.put("totalReceivedFromSells", totalMoneyReceived);
        result.put("realizedAndUnrealizedProfitLoss", profitLoss);
        result.put("calculationTimestamp", LocalDateTime.now().toString());

        if (AppConfig.isEnabled()) {
            walletCalcCacheDAO.saveStockPL(walletId, stockTicker, result);
        }

        return result;
    }

    public JSONObject getWalletGlobalProfitLossViaAggregation(String walletIdStr) {
        // Validate ID and Get Basic Wallet Info (currency)
        Wallet wallet = getWalletById(walletIdStr); // Handles validation

        if (AppConfig.isEnabled()) {
            Optional<JSONObject> cachedPL = walletCalcCacheDAO.findGlobalPL(wallet.getWalletId());
            if (cachedPL.isPresent()) {
                System.out.println("Wallet " + walletIdStr + " cached: " + cachedPL);
                return cachedPL.get();
            }
        }
        // Get Total Current Holdings Value via DAO Aggregation
        BigDecimal totalCurrentHoldingsValue = holdingsDAO.getTotalHoldingsValueByAggregation(wallet.getWalletId());

        // Get Total Buy/Sell Amounts via DAO Aggregation
        Document buySellTotalsDoc = transactionDAO.aggregateBuySellTotals(wallet.getWalletId());

        // Handle potential null result from transaction aggregation
        BigDecimal totalMoneySpentOnBuys = BigDecimal.ZERO;
        BigDecimal totalMoneyReceivedFromSells = BigDecimal.ZERO;
        if (buySellTotalsDoc != null) {
            totalMoneySpentOnBuys = buySellTotalsDoc.get("totalSpentOnBuys", BigDecimal.class); // Use get with type
            totalMoneyReceivedFromSells = buySellTotalsDoc.get("totalReceivedFromSells", BigDecimal.class);
        } else {
            System.err.println("Warning: Could not aggregate transaction totals for wallet " + walletIdStr);
            // Decide how to proceed - maybe return error in JSON? For now, defaults to zero.
        }


        // Calculate Global Profit/Loss
        // Global P/L = (Total Current Holdings Value + Total Money Received from Sells) - Total Money Spent on Buys
        BigDecimal globalProfitLoss = totalCurrentHoldingsValue
                .add(totalMoneyReceivedFromSells)
                .subtract(totalMoneySpentOnBuys);

        // Build Response JSON
        JSONObject result = new JSONObject();
        result.put("walletId", walletIdStr);
        result.put("currency", wallet.getCurrencyCode());
        result.put("calculationMethod", "Aggregation"); // Indicate method used
        result.put("totalCurrentHoldingsValue", totalCurrentHoldingsValue);
        result.put("totalSpentOnBuys", totalMoneySpentOnBuys);
        result.put("totalReceivedFromSells", totalMoneyReceivedFromSells);
        result.put("globalRealizedAndUnrealizedProfitLoss", globalProfitLoss);
        result.put("calculationTimestamp", LocalDateTime.now().toString());

        if (AppConfig.isEnabled()) {
            walletCalcCacheDAO.saveGlobalPL(wallet.getWalletId(), result);
        }

        return result;
    }
}
