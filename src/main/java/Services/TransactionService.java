package Services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

    public TransactionService(MongoDatabase database) {
        this.investorDAO = new InvestorDAO(database);
        this.stockDAO = new StockDAO(database);
        this.transactionDAO = new TransactionDAO(database);
        this.holdingsDAO = new HoldingsDAO(database);

        this.investorCacheDAO = new InvestorCacheDAO();
        this.transactionCacheDAO = new TransactionCacheDAO();
        this.holdingCacheDAO = new HoldingCacheDAO();

        this.walletCalcCacheDAO = new WalletCalculationCacheDAO();
        this.reportCacheDAO = new ReportCacheDAO();
    }

    /**
     * Permet à un investisseur (via le wallet) d’acheter une action de Stock.
     * Vérifie les fonds, déduit le montant, crée une transaction et met à jour le holding.
     */
    public Transaction buyStock(String walletId, String stockTicker, BigDecimal quantity) {
        Wallet wallet = investorDAO.getWalletById(walletId);
        if(wallet == null) {
            throw new RuntimeException("Wallet not found : " + walletId);
        }

        Stock stock = stockDAO.findByStockTicker(stockTicker);
        if(stock == null) {
            throw new RuntimeException("Stock not found : " + stockTicker);
        }

        BigDecimal price = stock.getLastPrice();
        BigDecimal totalCost = price.multiply(quantity);

        if(wallet.getBalance().compareTo(totalCost) < 0) {
            throw new RuntimeException("Insufficient funds. Necessary : " + totalCost + ", available : " + wallet.getBalance());
        }

        wallet.setBalance(wallet.getBalance().subtract(totalCost));
        investorDAO.updateWallet(wallet);

        // Création de la transaction
        Transaction transaction = new Transaction();
        transaction.setPriceAtTransaction(price);
        transaction.setQuantity(quantity);
        transaction.setTransactionTypesId("BUY"); // TODO
        transaction.setTransactionStatusId("COMPLETED"); // TODO
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setStockId(stock.getStockTicker());
        transaction.setWalletId(wallet.getWalletId());
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionDAO.save(transaction);

        // on cherche si le wallet détient déjà ce stock
        Holding holding = holdingsDAO.findByWalletIdAndStockTicker(wallet.getWalletId(), stock.getStockTicker());
        if (holding != null) {
            // Si le holding existe, on met à jour la quantity, totalBuyCost et lastUpdated
            holding.setQuantity(holding.getQuantity().add(quantity));
            holding.setTotalBuyCost(holding.getTotalBuyCost().add(totalCost));
            holding.setLastUpdated(LocalDateTime.now());
            holdingsDAO.update(holding);
        } else {
            // Sinon, on crée un nouveau holding
            holding = new Holding();
            holding.setWalletId(wallet.getWalletId());
            holding.setStockTicker(stock.getStockTicker());
            holding.setQuantity(quantity);
            holding.setTotalBuyCost(totalCost);
            holding.setLastUpdated(LocalDateTime.now());
            holdingsDAO.save(holding);
        }

        if (AppConfig.isEnabled()) {
            Investor investor = investorDAO.findInvestorByWalletId(walletId);
            if(investor != null) {
                investorCacheDAO.delete(investor.getInvestorId().toString());
                System.out.println("Cache invalidated for investor: " + investor.getInvestorId());
            } else {
                System.err.println("Warning: Could not find investor to invalidate cache for wallet: " + walletId);
            }

            transactionCacheDAO.invalidateByWalletId(wallet.getWalletId());

            holdingCacheDAO.saveOrUpdateIndividual(holding, AppConfig.CACHE_TTL);
            holdingCacheDAO.invalidateByWalletId(wallet.getWalletId());

            walletCalcCacheDAO.invalidateWalletValue(wallet.getWalletId());
            walletCalcCacheDAO.invalidateGlobalPL(wallet.getWalletId());
            walletCalcCacheDAO.invalidateStockPL(wallet.getWalletId(), stockTicker);
            System.out.println("Caches calculations invalidated for wallet: " + wallet.getWalletId());
        }
        return transaction;
    }


    /**
     * Permet à un investisseur de vendre une quantité d’un actif.
     * La vente ajoute des fonds au portefeuille et met à jour le holding correspondant.
     */
    public Transaction sellStock(String walletId, String stockTicker, BigDecimal quantity) {
        Wallet wallet = investorDAO.getWalletById(walletId);
        if (wallet == null) {
            throw new RuntimeException("Wallet not found : " + walletId);
        }

        Stock stock = stockDAO.findByStockTicker(stockTicker);
        if (stock == null) {
            throw new RuntimeException("Stock not found : " + stockTicker);
        }

        System.out.println("wallet found : "+ wallet + " stock found : "+ stock);
        System.out.println("walletId : "+ wallet.getWalletId() + " stockTicker : "+ stock.getStockTicker());
        Holding holding = holdingsDAO.findByWalletIdAndStockTicker(wallet.getWalletId(), stock.getStockTicker());
        System.out.println("holding found : "+ holding);
        if (holding == null || holding.getQuantity().compareTo(quantity) < 0) {
            throw new RuntimeException("Insufficient quantity in holding. Available : " + holding.getQuantity() + ", requested : " + quantity);
        }

        BigDecimal price = stock.getLastPrice();
        BigDecimal totalSellCost = price.multiply(quantity);
//
//        System.out.println("Balance before sell: " + wallet.getBalance());
//        System.out.println("Total Sell Cost: " + totalSellCost);
        wallet.setBalance(wallet.getBalance().add(totalSellCost));
//        System.out.println("Balance after sell: " + wallet.getBalance());
        investorDAO.updateWallet(wallet);

        // Création de la transaction
        Transaction transaction = new Transaction();
        transaction.setPriceAtTransaction(price);
        transaction.setQuantity(quantity);
        transaction.setTransactionTypesId("SELL"); //TODO
        transaction.setTransactionStatusId("COMPLETED"); //TODO
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setStockId(stock.getStockTicker());
        transaction.setWalletId(wallet.getWalletId());
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionDAO.save(transaction);

        // On cherche si le wallet détient déjà ce stock
        if (holding != null) {
            // Si le holding existe, on met à jour la quantity, totalSellCost et lastUpdated
            holding.setQuantity(holding.getQuantity().subtract(quantity));
            holding.setTotalSellCost(holding.getTotalSellCost().add(totalSellCost));
            holding.setLastUpdated(LocalDateTime.now());
            holdingsDAO.update(holding);
        } else {
            // Sinon, on crée un nouveau holding
            holding = new Holding();
            holding.setWalletId(wallet.getWalletId());
            holding.setStockTicker(stock.getStockTicker());
            holding.setQuantity(quantity);
            holding.setTotalSellCost(totalSellCost);
            holding.setLastUpdated(LocalDateTime.now());
            holdingsDAO.save(holding);
        }

        if (AppConfig.isEnabled()) {
            Investor investor = investorDAO.findInvestorByWalletId(walletId);
            if(investor != null) {
                investorCacheDAO.delete(investor.getInvestorId().toString());
                System.out.println("Cache invalidated for investor: " + investor.getInvestorId());
            } else {
                System.err.println("Warning: Could not find investor to invalidate cache for wallet: " + walletId);
            }

            transactionCacheDAO.invalidateByWalletId(wallet.getWalletId());

            holdingCacheDAO.saveOrUpdateIndividual(holding, AppConfig.CACHE_TTL);
            holdingCacheDAO.invalidateByWalletId(wallet.getWalletId());

            walletCalcCacheDAO.invalidateWalletValue(wallet.getWalletId());
            walletCalcCacheDAO.invalidateGlobalPL(wallet.getWalletId());
            walletCalcCacheDAO.invalidateStockPL(wallet.getWalletId(), stockTicker);
            System.out.println("Caches calculations invalidated for wallet: " + wallet.getWalletId());
        }
        return transaction;
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
    /*
    public List<Document> getMostTradedStocks(LocalDateTime start, LocalDateTime end, int limit) {
        // Add validation for limit if needed (e.g., limit > 0)
        if (limit <= 0) limit = 10; // Default limit
        return transactionDAO.aggregateStockTransactionCounts(start, end, limit);
    }*/

}