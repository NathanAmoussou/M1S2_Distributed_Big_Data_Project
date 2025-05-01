package Services;

import CacheDAO.InvestorCacheDAO;
import com.mongodb.client.MongoDatabase;
import Config.AppConfig;
import DAO.InvestorDAO;
import Models.Investor;
import Models.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class InvestorService {
    private final InvestorDAO investorDAO;
    private final MongoDatabase database;
    private final InvestorCacheDAO investorCacheDAO;

    public InvestorService(MongoDatabase database) {
        this.database = database;
        this.investorDAO = new InvestorDAO(database);
        this.investorCacheDAO = new InvestorCacheDAO();
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
}
