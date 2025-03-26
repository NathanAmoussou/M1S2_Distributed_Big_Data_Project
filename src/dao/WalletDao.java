package src.dao;

import src.model.Wallet;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.math.BigDecimal;
import java.util.List;

public class WalletDao implements GenericDao<Wallet> {

    private final MongoCollection<Document> collection;

    public WalletDao(MongoDatabase database) {
        this.collection = database.getCollection("wallets");
    }


    @Override
    public Wallet findById(String id) {
        Document doc = collection.find(eq("_id", id)).first();
        return doc != null ? documentToWallet(doc) : null;
    }

    private Wallet documentToWallet(Document doc) {
        Wallet w = new Wallet();
        w.setWalletId(doc.getString("_id"));
        w.setCurrencyCode(doc.getString("currencyCode"));
        w.setBalance(new BigDecimal(doc.getString("balance")));

        return w;
    }

    @Override
    public List<Wallet> findAll() {
        return List.of();
    }

    @Override
    public void save(Wallet wallet) {

    }

    @Override
    public void update(Wallet wallet) {

    }

    @Override
    public void deleteById(String id) {

    }
}
