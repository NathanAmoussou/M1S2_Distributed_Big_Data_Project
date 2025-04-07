package service;

import com.mongodb.client.MongoDatabase;
import dao.HoldingsDAO;
import dao.StockDAO;
import dao.TransactionDAO;
import dao.WalletDAO;
import model.Holdings;
import model.Stock;
import model.Transaction;
import model.Wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

public class InvestmentService {
    private final WalletDAO walletDAO;
    private final StockDAO stockDAO;
    private final TransactionDAO transactionDAO;
    private final HoldingsDAO holdingsDAO;

    public InvestmentService(MongoDatabase database) {
        this.walletDAO = new WalletDAO(database);
        this.stockDAO = new StockDAO(database);
        this.transactionDAO = new TransactionDAO(database.getCollection("transactions"));
        this.holdingsDAO = new HoldingsDAO(database.getCollection("holdings"));
    }

    /**
     * Permet à un investisseur d’acheter un actif (ex. action).
     * Vérifie les fonds, déduit le montant, crée une transaction et met à jour le holding.
     */
    public Transaction investInAsset(String investorId, String stockTicker, int quantity) {
        Wallet wallet = walletDAO.findById(investorId);
        if(wallet == null) {
            throw new RuntimeException("Portefeuille non trouvé pour l’investisseur : " + investorId);
        }

        Stock stock = stockDAO.findByStockTicker(stockTicker);
        if(stock == null) {
            throw new RuntimeException("Action non trouvée : " + stockTicker);
        }

        BigDecimal price = stock.getLastPrice();
        BigDecimal totalCost = price.multiply(new BigDecimal(quantity));

        if(wallet.getBalance().compareTo(totalCost) < 0) {
            throw new RuntimeException("Fonds insuffisants. Nécessaire : " + totalCost + ", disponible : " + wallet.getBalance());
        }

        wallet.setBalance(wallet.getBalance().subtract(totalCost));
        walletDAO.update(wallet);

        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setPriceAtTransaction(price);
        transaction.setQuantity(quantity);
        transaction.setTransactionTypesId("BUY"); //TODO
        transaction.setTransactionStatusId("COMPLETED"); //TODO
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setStockId(stock.getStockTicker());
        transaction.setWalletId(wallet.getWalletId());
        transaction.setUpdatedAt(LocalDateTime.now());

        transactionDAO.save(transaction);

        Holdings holding = null;
        for(Holdings h : holdingsDAO.findAll()) {
            if(h.getWalletId().equals(wallet.getWalletId()) && h.getStockId().equals(stock.getStockTicker())){
                holding = h;
                break;
            }
        }

        if(holding == null) {
            // Création d’un nouveau holding
            holding = new Holdings();
            holding.setHoldingsId(UUID.randomUUID().toString());
            holding.setStockId(stock.getStockTicker());
            holding.setWalletId(wallet.getWalletId());
            holding.setQuantity(quantity);
            holding.setAveragePurchasePrice(price);
            holdingsDAO.save(holding);
        } else {
            // Mise à jour du holding existant avec calcul du nouveau prix moyen
            BigDecimal oldTotalCost = holding.getAveragePurchasePrice().multiply(new BigDecimal(holding.getQuantity()));
            BigDecimal newTotalCost = oldTotalCost.add(totalCost);
            int newQuantity = holding.getQuantity() + quantity;
            BigDecimal newAvgPrice = newTotalCost.divide(new BigDecimal(newQuantity), RoundingMode.HALF_UP);

            holding.setQuantity(newQuantity);
            holding.setAveragePurchasePrice(newAvgPrice);
            holdingsDAO.update(holding);
        }

        return transaction;
    }
}