package src.dao;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import src.model.Country;

import java.util.ArrayList;
import java.util.List;

public class CountryDAO implements GenericDAO<Country> {

    private final MongoCollection<Document> collection;

    public CountryDAO(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public Country findById(String id) {
        Document doc = collection.find(new Document("_id", id)).first();
        return doc != null ? documentToCountry(doc) : null;
    }

    private Country documentToCountry(Document doc) {
        Country country = new Country();
        country.setCountryId(doc.getString("_id"));
        country.setCountryCode(doc.getString("countryCode"));
        country.setCountryName(doc.getString("countryName"));
        return country;
    }

    @Override
    public List<Country> findAll() {
        List<Country> result = new ArrayList<>();
        for (Document doc : collection.find()) {
            result.add(documentToCountry(doc));
        }
        return result;
    }

    @Override
    public void save(Country country) {
        Document doc = new Document("_id", country.getCountryId())
                .append("countryCode", country.getCountryCode())
                .append("countryName", country.getCountryName());
        collection.insertOne(doc);

    }

    @Override
    public void update(Country country) {
        Document doc = new Document("countryCode", country.getCountryCode())
                .append("countryName", country.getCountryName());
        collection.updateOne(new Document("_id", country.getCountryId()), new Document("$set", doc));

    }

    @Override
    public void deleteById(String id) {
        collection.deleteOne(new Document("_id", id));

    }
}
