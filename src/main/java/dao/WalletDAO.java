package dao;

import model.Wallet;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


import static com.mongodb.client.model.Filters.eq;

public class WalletDAO implements GenericDAO<Wallet> {

    private final MongoCollection<Document> collection;

    public WalletDAO(MongoDatabase database) {
        this.collection = database.getCollection("wallets");
    }

    @Override
    public Wallet findById(String id) {
        Document doc = collection.find(eq("_id", id)).first();
        return doc != null ? documentToWallet(doc) : null;
    }

    @Override
    public List<Wallet> findAll() {
        List<Wallet> result = new ArrayList<>();
        for (Document doc : collection.find()) {
            result.add(documentToWallet(doc));
        }
        return result;
    }

    @Override
    public void save(Wallet wallet) {
        Document doc = new Document("_id", wallet.getWalletId())
                .append("currencyCode", wallet.getCurrencyCode())
                .append("balance", wallet.getBalance().toString())
                .append("investorId", wallet.getInvestorId())
                .append("walletTypeKey", wallet.getWalletTypeId());
        collection.insertOne(doc);
    }

    @Override
    public void update(Wallet wallet) {
        Document doc = new Document("currencyCode", wallet.getCurrencyCode())
                .append("balance", wallet.getBalance().toString())
                .append("investorId", wallet.getInvestorId())
                .append("walletTypeKey", wallet.getWalletTypeId());
        collection.updateOne(eq("_id", wallet.getWalletId()), new Document("$set", doc));
    }

    @Override
    public void deleteById(String id) {
        collection.deleteOne(eq("_id", id));
    }


    private Wallet documentToWallet(Document doc) {
        Wallet w = new Wallet();
        w.setWalletId(doc.getString("_id"));
        w.setCurrencyCode(doc.getString("currencyCode"));
        w.setBalance(new BigDecimal(doc.getString("balance")));
        w.setInvestorId(doc.getString("investorId"));
        w.setWalletTypeId(doc.getString("walletTypeKey"));
        return w;
    }
}
