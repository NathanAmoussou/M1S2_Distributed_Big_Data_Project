package service;

import com.mongodb.client.MongoDatabase;
import config.AppConfig;
import dao.InvestorDAO;
import model.Investor;
import model.Wallet;
import model.Address;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import util.RedisCacheService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class InvestorService {
    private final InvestorDAO investorDAO;
    private final MongoDatabase database;

    public InvestorService(MongoDatabase database) {
        this.database = database;
        this.investorDAO = new InvestorDAO(database);
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
        if (AppConfig.isEnabled()) {
            RedisCacheService.setCache(
                    "investor:" + investor.getInvestorId(),
                    investor.toString(), AppConfig.CACHE_TTL);
        }

        return investor;
    }

    public Investor getInvestor(String investorId) {
        try {
            if (AppConfig.isEnabled()) {
                String cachedInvestor = RedisCacheService.getCache("investor:" + investorId);
                if (cachedInvestor != null) {
                    JSONObject json = new JSONObject(cachedInvestor);
                    Investor cached = new Investor(json);
                    System.out.println("Investisseur récupéré depuis le cache.");
                    return cached;
                }
            }
            Investor investor = investorDAO.findById(investorId);
            if (investor != null && AppConfig.isEnabled()) {
                RedisCacheService.setCache("investor:" + investorId, investor.toString(), AppConfig.CACHE_TTL);
            }
            return investor;
        } catch(Exception e) {
            System.err.println("Erreur lors de la lecture de l'investisseur: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<Investor> getAllInvestors() {
        return investorDAO.findAll();
    }

    /**
     * Ajoute des fonds au portefeuille d’un investisseur.
     */
    public Wallet addFunds(String investorId, BigDecimal amount) {
        Investor investor = investorDAO.findById(investorId);
        if (investor == null) {
            throw new RuntimeException("Investisseur non trouvé pour l'ID " + investorId);
        }

        // Assuming the first wallet is the primary one to be updated
        Wallet wallet = investor.getWallets().stream().findFirst().orElse(null);
        if (wallet == null) {
            throw new RuntimeException("Aucun portefeuille trouvé pour l’investisseur " + investorId);
        }

        wallet.setBalance(wallet.getBalance().add(amount));

        // Save the updated investor (MongoDB will update the document with the new wallet balance)
        investorDAO.update(investor);

        // Update cache if enabled
        if (AppConfig.isEnabled()) {
            RedisCacheService.setCache(
                    "investor:" + investorId,
                    investor.toString(),
                    AppConfig.CACHE_TTL
            );
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
            RedisCacheService.setCache(
                    "investor:" + investor.getInvestorId(),
                    investor.toString(),
                    AppConfig.CACHE_TTL
            );
        }

        return investor;
    }
}
