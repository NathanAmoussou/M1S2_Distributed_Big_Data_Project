package service;

import com.mongodb.client.MongoDatabase;
import config.AppConfig;
import dao.InvestorDAO;
import dao.WalletDAO;
import model.Investor;
import model.Wallet;
import org.json.JSONObject;
import util.RedisCacheService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class InvestorService {
    private final InvestorDAO investorDAO;
    private final WalletDAO walletDAO;
    private final MongoDatabase database;

    public InvestorService(MongoDatabase database) {
        this.database = database;
        this.investorDAO = new InvestorDAO(database);
        this.walletDAO = new WalletDAO(database);
    }

    /**
     * Crée un nouvel investisseur et son portefeuille associé.
     */
    public Investor createInvestor(Investor investor) {
        // TODO: id
        if(investor.getInvestorId() == null || investor.getInvestorId().isEmpty()){
            investor.setInvestorId(UUID.randomUUID().toString());
        }
        investor.setCreationDate(LocalDateTime.now());
        investor.setLastUpdateDate(LocalDateTime.now());

        investorDAO.save(investor);

        // TODO: id
        Wallet wallet = new Wallet();
        wallet.setWalletId(investor.getInvestorId());
        wallet.setInvestorId(investor.getInvestorId());
        wallet.setCurrencyCode("USD"); // devise par défaut
        wallet.setBalance(new BigDecimal("0.0"));
        // TODO: id
        wallet.setWalletTypeId("default");

        walletDAO.save(wallet);
        if(AppConfig.isEnabled()){
            RedisCacheService.setCache("investor:" + investor.getInvestorId(), RedisCacheService.investorToJson(investor).toString(), AppConfig.CACHE_TTL);
        }

        return investor;
    }

    public Investor getInvestor(String investorId) {
        try {
            if(AppConfig.isEnabled()){
                String cachedInvestor = RedisCacheService.getCache("investor:" + investorId);
                if(cachedInvestor != null) {
                    JSONObject json = new JSONObject(cachedInvestor);
                    Investor cached = RedisCacheService.jsonToInvestor(json);
                    System.out.println("Investisseur récupéré depuis le cache.");
                    return cached;
                }
            }
            Investor investor = investorDAO.findById(investorId);
            if (investor != null && AppConfig.isEnabled()){
                RedisCacheService.setCache("investor:" + investorId, RedisCacheService.investorToJson(investor).toString(), AppConfig.CACHE_TTL);
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
        Wallet wallet = walletDAO.findById(investorId);
        if(wallet == null){
            throw new RuntimeException("Portefeuille non trouvé pour l’investisseur " + investorId);
        }
        wallet.setBalance(wallet.getBalance().add(amount));
        walletDAO.update(wallet);
        return wallet;
    }

    public Investor updateInvestor(Investor investor) {
        investor.setLastUpdateDate(LocalDateTime.now());
        investorDAO.update(investor);
        if(AppConfig.isEnabled()){
            RedisCacheService.setCache("investor:" + investor.getInvestorId(), RedisCacheService.investorToJson(investor).toString(), AppConfig.CACHE_TTL);
        }
        return investor;
    }
}
