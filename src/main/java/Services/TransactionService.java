package Services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;import java.util.Optional;
import java.util.stream.Collectors;

import CacheDAO.*;import java.util.Optional;
import java.util.stream.Collectors;

import CacheDAO.*;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoDatabase;

import Config.AppConfig;
import DAO.HoldingsDAO;
import DAO.InvestorDAO;
import DAO.StockDAO;
import DAO.TransactionDAO;
import Models.Holding;
import Models.Investor;
import Models.Stock;
import Models.Transaction;
import Models.Wallet;

public class TransactionService {
    private final InvestorDAO investorDAO;
    private final StockDAO stockDAO;
    private final TransactionDAO transactionDAO;
    private final HoldingsDAO holdingsDAO;

    private final InvestorCacheDAO investorCacheDAO;
    private final TransactionCacheDAO transactionCacheDAO;
    private final HoldingCacheDAO holdingCacheDAO;

    private final WalletCalculationCacheDAO walletCalcCacheDAO;
    private final ReportCacheDAO reportCacheDAO;

    private final MongoClient mongoClient;

    public TransactionService(MongoDatabase database, MongoClient mongoClient) {
        this.investorDAO = new InvestorDAO(database);
        this.stockDAO = new StockDAO(database);
        this.transactionDAO = new TransactionDAO(database);
        this.holdingsDAO = new HoldingsDAO(database);

        this.investorCacheDAO = new InvestorCacheDAO();
        this.transactionCacheDAO = new TransactionCacheDAO();
        this.holdingCacheDAO = new HoldingCacheDAO();

        this.walletCalcCacheDAO = new WalletCalculationCacheDAO();
        this.reportCacheDAO = new ReportCacheDAO();

        this.mongoClient = mongoClient;
    }

    /**
     * Permet à un investisseur (via le wallet) d’acheter une action de Stock.
     * S'assure de l'atomicité avec les transactions MongoDB
     *      (pour éviter les problèmes de concurrence, comme plusieurs achats simultanés).
     * Vérifie les fonds, déduit le montant, crée une transaction et met à jour le holding.
     */
    public Transaction buyStock(String walletIdStr, String stockTicker, BigDecimal quantity) {
        if (!ObjectId.isValid(walletIdStr)) {
            throw new IllegalArgumentException("Invalid walletId format: " + walletIdStr);
        }
        ObjectId walletObjId = new ObjectId(walletIdStr);
        if (stockTicker == null || stockTicker.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock ticker cannot be empty.");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive value.");
        }

        // MongoDB Transaction creation
        final ClientSession clientSession = mongoClient.startSession();
        TransactionOptions txnOptions = TransactionOptions.builder()
                .readPreference(ReadPreference.primary())
                .readConcern(ReadConcern.MAJORITY) // Or LOCAL
                .writeConcern(WriteConcern.MAJORITY)
                .build();

        Transaction transactionResult = null;
        AtomicReference<Holding> finalHoldingState = new AtomicReference<>(); // To cache after commit

        try {
            transactionResult = clientSession.withTransaction(() -> {
                // Execute operations within the mongoDB transaction atomic by passing the session

                // Get Wallet with session
                Wallet wallet = investorDAO.getWalletById(clientSession, walletIdStr); // MODIFIED DAO CALL
                if (wallet == null) {
                    throw new RuntimeException("Wallet not found: " + walletIdStr);
                }

                // Get Stock with session
                Stock stock = stockDAO.findByStockTicker(clientSession, stockTicker); // MODIFIED DAO CALL
                if (stock == null) {
                    throw new RuntimeException("Stock not found: " + stockTicker);
                }

                // Check Funds
                BigDecimal price = stock.getLastPrice();
                BigDecimal totalCost = price.multiply(quantity);
                if (wallet.getBalance().compareTo(totalCost) < 0) {
                    throw new RuntimeException("Insufficient funds. Necessary: " + totalCost + ", available: " + wallet.getBalance());
                }

                // Update Wallet Balance session
                wallet.setBalance(wallet.getBalance().subtract(totalCost));
                investorDAO.updateWallet(clientSession, wallet);

                // create and save Transaction with session
                Transaction transaction = new Transaction();
                transaction.setPriceAtTransaction(price);
                transaction.setQuantity(quantity);
                transaction.setTransactionTypesId("BUY");
                transaction.setTransactionStatusId("COMPLETED");
                transaction.setCreatedAt(LocalDateTime.now());
                transaction.setStockId(stock.getStockTicker());
                transaction.setWalletId(wallet.getWalletId());
                transaction.setUpdatedAt(LocalDateTime.now());
                transactionDAO.save(clientSession, transaction);

                // Find or create/update Holding with session
                Holding holding = holdingsDAO.findByWalletIdAndStockTicker(clientSession, wallet.getWalletId(), stock.getStockTicker()); // MODIFIED DAO CALL
                if (holding != null) {
                    holding.setQuantity(holding.getQuantity().add(quantity));
                    holding.setTotalBuyCost(holding.getTotalBuyCost().add(totalCost)); // Update buy cost
                    holding.setTotalSellCost(holding.getTotalSellCost() != null ? holding.getTotalSellCost() : BigDecimal.ZERO); // Ensure not null
                    holding.setLastUpdated(LocalDateTime.now());
                    holdingsDAO.update(clientSession, holding);
                } else {
                    holding = new Holding();
                    holding.setWalletId(wallet.getWalletId());
                    holding.setStockTicker(stock.getStockTicker());
                    holding.setQuantity(quantity);
                    holding.setTotalBuyCost(totalCost);
                    holding.setTotalSellCost(BigDecimal.ZERO); // Initialize sell cost
                    holding.setLastUpdated(LocalDateTime.now());
                    holdingsDAO.save(clientSession, holding);
                }
                // Assign to outer scope variable for caching after commit
                finalHoldingState.set(holding);
                return transaction;
            }, txnOptions);

        } catch (Exception e) {
            // Exception occurred, transaction was implicitly aborted by withTransaction
            System.err.println("Transaction failed: " + e.getMessage());
            // Re-throw or handle specific exceptions as needed
            throw new RuntimeException("Buy transaction failed: " + e.getMessage(), e);
        } finally {
            clientSession.close(); // close the session to release resources
        }


        // --- Cache Invalidation/Update (AFTER successful commit) ---
        if (AppConfig.isEnabled() && transactionResult != null && finalHoldingState.get() != null) {
            try {
                Investor investor = investorDAO.findInvestorByWalletId(walletIdStr); // Fetch investor AFTER txn
                if (investor != null) {
                    investorCacheDAO.delete(investor.getInvestorId().toString()); // Invalidate investor
                    System.out.println("Cache invalidated for investor: " + investor.getInvestorId());
                }
                transactionCacheDAO.invalidateByWalletId(walletObjId); // Invalidate transactions list
                System.out.println("Cache invalidated for transactions: " + walletObjId);

                // Update individual holding and invalidate list
                holdingCacheDAO.saveOrUpdateIndividual(finalHoldingState.get(), AppConfig.CACHE_TTL);
                holdingCacheDAO.invalidateByWalletId(walletObjId);
                System.out.println("Cache updated/invalidated for holdings: " + walletObjId);
            } catch (Exception cacheEx) {
                System.err.println("Warning: Cache operation failed after successful transaction: " + cacheEx.getMessage());
            }
        }

        return transactionResult; // Return the created transaction
    }

    /**
     * Permet à un investisseur de vendre une quantité d’un actif.
     * La vente ajoute des fonds au portefeuille et met à jour le holding correspondant.
     * S'assure de l'atomicité avec les transactions MongoDB
     *     (pour éviter les problèmes de concurrence, comme plusieurs ventes simultanées)
     */
    public Transaction sellStock(String walletIdStr, String stockTicker, BigDecimal quantity) {
        if (!ObjectId.isValid(walletIdStr)) {
            throw new IllegalArgumentException("Invalid walletId format: " + walletIdStr);
        }
        ObjectId walletObjId = new ObjectId(walletIdStr);
        if (stockTicker == null || stockTicker.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock ticker cannot be empty.");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive value.");
        }

        // MongoDB Transaction creation
        final ClientSession clientSession = mongoClient.startSession();
        TransactionOptions txnOptions = TransactionOptions.builder()
                .readPreference(ReadPreference.primary())
                .readConcern(ReadConcern.MAJORITY)
                .writeConcern(WriteConcern.MAJORITY)
                .build();

        Transaction transactionResult = null;
        AtomicReference<Holding> finalHoldingState = new AtomicReference<>();

        try {
            transactionResult = clientSession.withTransaction(() -> {
                //get Wallet
                Wallet wallet = investorDAO.getWalletById(clientSession, walletIdStr);
                if (wallet == null) {
                    throw new RuntimeException("Wallet not found: " + walletIdStr);
                }

                // get Stock
                Stock stock = stockDAO.findByStockTicker(clientSession, stockTicker);
                if (stock == null) {
                    throw new RuntimeException("Stock not found: " + stockTicker);
                }

                //get Holding and check quantity
                Holding holding = holdingsDAO.findByWalletIdAndStockTicker(clientSession, wallet.getWalletId(), stock.getStockTicker()); // MODIFIED
                if (holding == null || holding.getQuantity().compareTo(quantity) < 0) {
                    throw new RuntimeException("Insufficient quantity in holding. Available: " +
                            (holding != null ? holding.getQuantity() : "0") + ", requested: " + quantity);
                }

                // calculate sell value and update wallet balance
                BigDecimal price = stock.getLastPrice();
                BigDecimal totalProceeds = price.multiply(quantity);
                wallet.setBalance(wallet.getBalance().add(totalProceeds));
                investorDAO.updateWallet(clientSession, wallet);

                // create and save transaction
                Transaction transaction = new Transaction();
                transaction.setPriceAtTransaction(price);
                transaction.setQuantity(quantity);
                transaction.setTransactionTypesId("SELL");
                transaction.setTransactionStatusId("COMPLETED");
                transaction.setCreatedAt(LocalDateTime.now());
                transaction.setStockId(stock.getStockTicker());
                transaction.setWalletId(wallet.getWalletId());
                transaction.setUpdatedAt(LocalDateTime.now());
                transactionDAO.save(clientSession, transaction);

                // update holding
                holding.setQuantity(holding.getQuantity().subtract(quantity));
                // Ensure buy cost is not null before updating sell cost
                holding.setTotalBuyCost(holding.getTotalBuyCost() != null ? holding.getTotalBuyCost() : BigDecimal.ZERO);
                holding.setTotalSellCost(holding.getTotalSellCost().add(totalProceeds)); // Update sell proceeds
                holding.setLastUpdated(LocalDateTime.now());
                holdingsDAO.update(clientSession, holding);

                finalHoldingState.set(holding);
                return transaction;
            }, txnOptions);

        } catch (Exception e) {
            System.err.println("Transaction failed: " + e.getMessage());
            throw new RuntimeException("Sell transaction failed: " + e.getMessage(), e);
        } finally {
            clientSession.close();
        }

        // Cache Invalidation/Update (AFTER successful commit)
        if (AppConfig.isEnabled() && transactionResult != null && finalHoldingState.get() != null) {
            try {
                Investor investor = investorDAO.findInvestorByWalletId(walletIdStr); // Fetch investor AFTER txn
                if (investor != null) {
                    investorCacheDAO.delete(investor.getInvestorId().toString()); // Invalidate investor
                    System.out.println("Cache invalidated for investor: " + investor.getInvestorId());
                }
                transactionCacheDAO.invalidateByWalletId(walletObjId); // Invalidate transactions list
                System.out.println("Cache invalidated for transactions: " + walletObjId);

                // Update individual holding and invalidate list
                holdingCacheDAO.saveOrUpdateIndividual(finalHoldingState.get(), AppConfig.CACHE_TTL);
                holdingCacheDAO.invalidateByWalletId(walletObjId);
                System.out.println("Cache updated/invalidated for holdings: " + walletObjId);
            } catch (Exception cacheEx) {
                System.err.println("Warning: Cache operation failed after successful transaction: " + cacheEx.getMessage());
            }
        }

        return transactionResult;
    }


    /**
     * Retrieves transactions for a specific wallet optionally filtered by date.
     */
    public List<Transaction> getTransactionsForWallet(String walletIdStr, LocalDate startDate, LocalDate endDate) {
        if (!ObjectId.isValid(walletIdStr)) {
            throw new IllegalArgumentException("Invalid wallet ID format: " + walletIdStr);
        }
        ObjectId walletId = new ObjectId(walletIdStr);

        List<Transaction> allTransactions;

        if (AppConfig.isEnabled()) {
            Optional<List<Transaction>> cachedTransactions = transactionCacheDAO.findByWalletId(walletId);
            if (cachedTransactions.isPresent()) {
                allTransactions = cachedTransactions.get(); // Cache HIT
            } else {
                allTransactions = transactionDAO.getTransactionsByWalletId(walletId);

                // Sauvegarder la liste complète dans le cache
                if (allTransactions != null && AppConfig.isEnabled()) { // Vérifier null avant sauvegarde
                    transactionCacheDAO.saveByWalletId(walletId, allTransactions);
                }
            }
        } else {

            allTransactions = transactionDAO.getTransactionsByWalletId(walletId);
        }

        if (allTransactions == null) {
            return Collections.emptyList();
        }

        if (startDate == null && endDate == null) {
            return allTransactions;
        } else {
            LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
            LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

            return allTransactions.stream()
                    .filter(tx -> {
                        LocalDateTime createdAt = tx.getCreatedAt();
                        boolean afterStart = (startDateTime == null) || !createdAt.isBefore(startDateTime);
                        boolean beforeEnd = (endDateTime == null) || !createdAt.isAfter(endDateTime);
                        return afterStart && beforeEnd;
                    })
                    .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                    .collect(Collectors.toList());
        }
    }
    /*
    public List<Transaction> getTransactionsForWallet(String walletIdStr, LocalDate startDate, LocalDate endDate) {
        if (!ObjectId.isValid(walletIdStr)) {
            throw new IllegalArgumentException("Invalid wallet ID format: " + walletIdStr);
        }
        ObjectId walletId = new ObjectId(walletIdStr);

        List<Transaction> allTransactions;

        if (AppConfig.isEnabled()) {
            Optional<List<Transaction>> cachedTransactions = transactionCacheDAO.findByWalletId(walletId);
            if (cachedTransactions.isPresent()) {
                allTransactions = cachedTransactions.get(); // Cache HIT
            } else {
                allTransactions = transactionDAO.getTransactionsByWalletId(walletId);

                // Sauvegarder la liste complète dans le cache
                if (allTransactions != null && AppConfig.isEnabled()) { // Vérifier null avant sauvegarde
                    transactionCacheDAO.saveByWalletId(walletId, allTransactions);
                }
            }
        } else {

            allTransactions = transactionDAO.getTransactionsByWalletId(walletId);
        }

        if (allTransactions == null) {
            return Collections.emptyList();
        }

        if (startDate == null && endDate == null) {
            return allTransactions;
        } else {
            LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
            LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

            return allTransactions.stream()
                    .filter(tx -> {
                        LocalDateTime createdAt = tx.getCreatedAt();
                        boolean afterStart = (startDateTime == null) || !createdAt.isBefore(startDateTime);
                        boolean beforeEnd = (endDateTime == null) || !createdAt.isAfter(endDateTime);
                        return afterStart && beforeEnd;
                    })
                    .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                    .collect(Collectors.toList());
        }
    }
    /*
    public List<Transaction> getTransactionsForWallet(String walletIdStr, LocalDate startDate, LocalDate endDate) {
        if (!ObjectId.isValid(walletIdStr)) {
            throw new IllegalArgumentException("Invalid wallet ID format: " + walletIdStr);
        }
        ObjectId walletId = new ObjectId(walletIdStr);

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        return transactionDAO.findByWalletIdAndDateRange(walletId, startDateTime, endDateTime);
    }*/
    public List<Document> getMostTradedStocks(LocalDateTime start, LocalDateTime end, int limit) {
        if (limit <= 0) limit = 10;

        if (AppConfig.isEnabled()) {
            Optional<List<Document>> cachedReport = reportCacheDAO.findMostTraded(start, end, limit);
            if (cachedReport.isPresent()) {
                System.out.println("Most traded stocks found in cache: " + cachedReport.get().size());
                return cachedReport.get();
            }
        }

        List<Document> reportData = transactionDAO.aggregateStockTransactionCounts(start, end, limit);

        if (reportData != null && AppConfig.isEnabled()) {
            reportCacheDAO.saveMostTraded(start, end, limit, reportData);
        }

        return reportData == null ? Collections.emptyList() : reportData;
    }


}