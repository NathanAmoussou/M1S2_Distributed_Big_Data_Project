//package dao;
//
//import com.mongodb.client.MongoCollection;
//import org.bson.Document;
//import model.Address;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class AddresseDAO implements GenericDAO<Address> {
//
//    private final MongoCollection<Document> collection;
//
//    public AddresseDAO(MongoCollection<Document> collection) {
//        this.collection = collection;
//    }
//
//    @Override
//    public Address findById(String id) {
//       throw new UnsupportedOperationException("Address is not collection but embedded in Investor as List<Address>");
//    }
//
//
//
//    @Override
//
//    }
//
//    @Override
//    public void save(Address address) {
//        Document doc = new Document("_id", address.getAddressId())
//                .append("street", address.getStreet())
//                .append("city", address.getCity())
//                .append("countryId", address.getCountryId())
//                .append("postalCode", address.getPostalCode());
//        collection.insertOne(doc);
//
//    }
//
//    @Override
//    public void update(Address address) {
//        Document doc = new Document("street", address.getStreet())
//                .append("city", address.getCity())
//                .append("countryId", address.getCountryId())
//                .append("postalCode", address.getPostalCode());
//        collection.updateOne(new Document("_id", address.getAddressId()), new Document("$set", doc));
//    }
//
//    @Override
//    public void deleteById(String id) {
//        collection.deleteOne(new Document("_id", id));
//    }
//}
