package dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import model.Investor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InvestorDAO implements GenericDAO<Investor> {

    private final MongoCollection<Document> collection;

    public InvestorDAO(MongoDatabase database) {
        this.collection = database.getCollection("investors");
    }

    @Override
    public Investor findById(String id) {
        Document doc = collection.find(new Document("_id", id)).first();
        return doc != null ? documentToInvestor(doc) : null;
    }

    private Investor documentToInvestor(Document doc) {
        Investor investor = new Investor();
        investor.setInvestorId(doc.getString("_id"));
        investor.setUsername(doc.getString("username"));
        investor.setPassword(doc.getString("password"));
        investor.setName(doc.getString("name"));
        investor.setSurname(doc.getString("surname"));
        investor.setEmail(doc.getString("email"));
        investor.setPhoneNumber(doc.getString("phoneNumber"));

        investor.setCreationDate(LocalDateTime.ofInstant(doc.getDate("creationDate").toInstant(), java.time.ZoneId.systemDefault()));
        investor.setLastUpdateDate(LocalDateTime.ofInstant(doc.getDate("lastUpdateDate").toInstant(), java.time.ZoneId.systemDefault()));
        investor.setAddressId(doc.getString("addressId"));
        return investor;
    }

    @Override
    public List<Investor> findAll() {
        List<Investor> result = new ArrayList<>();
        for (Document doc : collection.find()) {
            System.out.println(doc.toJson());
            result.add(documentToInvestor(doc));
        }
        return result;
    }

    @Override
    public void save(Investor investor) {
        Document doc = new Document("_id", investor.getInvestorId())
                .append("username", investor.getUsername())
                .append("password", investor.getPassword())
                .append("name", investor.getName())
                .append("surname", investor.getSurname())
                .append("email", investor.getEmail())
                .append("phoneNumber", investor.getPhoneNumber())
                .append("creationDate", investor.getCreationDate())
                .append("lastUpdateDate", investor.getLastUpdateDate())
                .append("addressId", investor.getAddressId());
        collection.insertOne(doc);
    }

    @Override
    public void update(Investor investor) {
        Document doc = new Document("username", investor.getUsername())
                .append("password", investor.getPassword())
                .append("name", investor.getName())
                .append("surname", investor.getSurname())
                .append("email", investor.getEmail())
                .append("phoneNumber", investor.getPhoneNumber())
                .append("creationDate", investor.getCreationDate())
                .append("lastUpdateDate", investor.getLastUpdateDate())
                .append("addressId", investor.getAddressId());
        collection.updateOne(new Document("_id", investor.getInvestorId()), new Document("$set", doc));

    }

    @Override
    public void deleteById(String id) {
        collection.deleteOne(new Document("_id", id));


    }
}

