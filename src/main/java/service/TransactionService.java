package service;

import com.mongodb.client.MongoDatabase;
import config.AppConfig;
import dao.HoldingsDAO;
import dao.StockDAO;
import dao.TransactionDAO;
import dao.InvestorDAO;
import model.Holding;
import model.Stock;
import model.Transaction;
import model.Wallet;
import org.json.JSONArray;
import util.RedisCacheService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class InvestmentService {
    private final InvestorDAO investorDAO;
    private final StockDAO stockDAO;
    private final TransactionDAO transactionDAO;
    private final HoldingsDAO holdingsDAO;

    public InvestmentService(MongoDatabase database) {
        this.investorDAO = new InvestorDAO(database);
        this.stockDAO = new StockDAO(database);
        this.transactionDAO = new TransactionDAO(database.getCollection("transactions"));
        this.holdingsDAO = new HoldingsDAO(database.getCollection("holdings"));
    }

    /**
     * Permet à un investisseur (via le wallet) d’acheter une action de Stock.
     * Vérifie les fonds, déduit le montant, crée une transaction et met à jour le holding.
     */
    public Transaction investInStock(String walletId, String stockTicker, BigDecimal quantity) {
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
        transaction.setTransactionTypesId("BUY"); //TODO
        transaction.setTransactionStatusId("COMPLETED"); //TODO
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
        } else {
            // Sinon, on crée un nouveau holding
            holding = new Holding();
            holding.setWalletId(wallet.getWalletId());
            holding.setStockTicker(stock.getStockTicker());
            holding.setQuantity(quantity);
            holding.setTotalBuyCost(totalCost);
            holding.setLastUpdated(LocalDateTime.now());
        }

//        updateTransactionCache(wallet, transaction);
        return transaction;
    }


//    /**
//     * Permet à un investisseur de vendre une quantité d’un actif.
//     * La vente ajoute des fonds au portefeuille et met à jour le holding correspondant.
//     */
//    public Transaction sellAsset(String investorId, String stockTicker, int quantity) {
//        Wallet wallet = walletDAO.findById(investorId);
//        if(wallet == null) {
//            throw new RuntimeException("Portefeuille non trouvé pour l’investisseur : " + investorId);
//        }
//
//        // Recherche du holding pour cet actif
//        Holdings existingHolding = null;
//        List<Holdings> holdings = holdingsDAO.findByWalletId(wallet.getWalletId());
//        for (Holdings h : holdings) {
//            if(h.getStockId().equals(stockTicker)) {
//                existingHolding = h;
//                break;
//            }
//        }
//
//        if(existingHolding == null) {
//            throw new RuntimeException("Aucun actif " + stockTicker + " détenu par l’investisseur");
//        }
//        if(existingHolding.getQuantity() < quantity) {
//            throw new RuntimeException("Quantité insuffisante pour la vente. Vous détenez " + existingHolding.getQuantity());
//        }
//
//        // Récupération du cours actuel
//        Stock stock = stockDAO.findByStockTicker(stockTicker);
//        if(stock == null) {
//            throw new RuntimeException("Action non trouvée : " + stockTicker);
//        }
//        BigDecimal price = stock.getLastPrice();
//        BigDecimal saleAmount = price.multiply(new BigDecimal(quantity));
//
//        // Mise à jour du portefeuille : ajout des fonds issus de la vente
//        wallet.setBalance(wallet.getBalance().add(saleAmount));
//        walletDAO.update(wallet);
//
//        // Mise à jour du holding : diminution ou suppression
//        if(existingHolding.getQuantity() == quantity) {
//            holdingsDAO.deleteById(existingHolding.getHoldingsId());
//        } else {
//            existingHolding.setQuantity(existingHolding.getQuantity() - quantity);
//            holdingsDAO.update(existingHolding);
//        }
//
//        // Création de la transaction de vente
//        Transaction transaction = new Transaction();
//        transaction.setTransactionId(UUID.randomUUID().toString());
//        transaction.setPriceAtTransaction(price);
//        transaction.setQuantity(quantity);
//        transaction.setTransactionTypesId("SELL");
//        transaction.setTransactionStatusId("COMPLETED");
//        transaction.setCreatedAt(LocalDateTime.now());
//        transaction.setStockId(stockTicker);
//        transaction.setWalletId(wallet.getWalletId());
//        transaction.setUpdatedAt(LocalDateTime.now());
//        transactionDAO.save(transaction);
//
//
//        updateTransactionCache(wallet, transaction);
//        return transaction;
//
//    }

//    private void updateTransactionCache(Wallet wallet, Transaction transaction) {
//        if (AppConfig.isEnabled()) {
//            RedisCacheService.setCache(
//                    "wallet:" + wallet.getWalletId(),
//                    wallet.toString(),
//                    AppConfig.CACHE_TTL
//            );
//            List<Transaction> transactions = transactionDAO.findByWalletId(wallet.getWalletId());
//            JSONArray arr = new JSONArray();
//            for (Transaction transactionTemp : transactions) {
//                arr.put(transactionTemp.toString());
//            }
//            RedisCacheService.setCache("transactions:wallet:" + wallet.getWalletId(), arr.toString(), AppConfig.CACHE_TTL);
//        }
//    }


}