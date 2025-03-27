package src.dao;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import src.model.Address;

import java.util.ArrayList;
import java.util.List;

public class AddresseDAO implements GenericDAO<Address> {

    private final MongoCollection<Document> collection;

    public AddresseDAO(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public Address findById(String id) {
        Document doc = collection.find(new Document("_id", id)).first();
        return doc != null ? documentToAddress(doc) : null;
    }

    private Address documentToAddress(Document doc) {
        Address address = new Address();
        address.setAddressId(doc.getString("_id"));
        address.setStreet(doc.getString("street"));
        address.setCity(doc.getString("city"));
        address.setCountryId(doc.getString("countryId"));
        address.setPostalCode(doc.getString("postalCode"));
        return address;
    }

    @Override
    public List<Address> findAll() {
        List<Address> result = new ArrayList<>();
        for (Document doc : collection.find()) {
            result.add(documentToAddress(doc));
        }
        return result;
    }

    @Override
    public void save(Address address) {
        Document doc = new Document("_id", address.getAddressId())
                .append("street", address.getStreet())
                .append("city", address.getCity())
                .append("countryId", address.getCountryId())
                .append("postalCode", address.getPostalCode());
        collection.insertOne(doc);

    }

    @Override
    public void update(Address address) {
        Document doc = new Document("street", address.getStreet())
                .append("city", address.getCity())
                .append("countryId", address.getCountryId())
                .append("postalCode", address.getPostalCode());
        collection.updateOne(new Document("_id", address.getAddressId()), new Document("$set", doc));
    }

    @Override
    public void deleteById(String id) {
        collection.deleteOne(new Document("_id", id));
    }
}
