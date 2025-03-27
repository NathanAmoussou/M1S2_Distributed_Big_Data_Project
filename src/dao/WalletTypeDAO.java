package src.dao;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import src.model.WalletType;

import java.util.ArrayList;
import java.util.List;

public class WalletTypeDAO implements GenericDAO<WalletType> {
    private final MongoCollection<Document> collection;

    public WalletTypeDAO(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public WalletType findById(String id) {
        Document doc = collection.find(new Document("_id", id)).first();
        return doc != null ? documentToWalletType(doc) : null;
    }

    private WalletType documentToWalletType(Document doc) {
        WalletType walletType = new WalletType();
        walletType.setTypeDescription(doc.getString("typeDescription"));
        walletType.setWalletTypeKey(doc.getString("_id"));
        return walletType;
    }

    @Override
    public List<WalletType> findAll() {
        List<WalletType> result = new ArrayList<>();
        for (Document doc : collection.find()) {
            result.add(documentToWalletType(doc));
        }
        return result;
    }

    @Override
    public void save(WalletType walletType) {
        Document doc = new Document("_id", walletType.getWalletTypeKey())
                .append("typeDescription", walletType.getTypeDescription());
        collection.insertOne(doc);
    }

    @Override
    public void update(WalletType walletType) {
        Document doc = new Document("typeDescription", walletType.getTypeDescription());
        collection.updateOne(new Document("_id", walletType.getWalletTypeKey()), new Document("$set", doc));

    }

    @Override
    public void deleteById(String id) {
        collection.deleteOne(new Document("_id", id));
    }
}
