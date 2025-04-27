package dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import model.Investor;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class InvestorDAO implements GenericDAO<Investor> {

    private final MongoCollection<Document> collection;


    public InvestorDAO(MongoDatabase database) {
        this.collection = database.getCollection("investors");
        this.createUniqueIndexes(); // index sur username and email to avoid duplicates investors
    }

    private void createUniqueIndexes() {
        try {
            // Creating an index on "username" to be unique
            collection.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));

            // Creating an index on "email" to be unique
            collection.createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));

            System.out.println("Unique indexes on 'username' and 'email' created successfully.");
        } catch (Exception e) {
            System.err.println("Error creating indexes: " + e.getMessage());
        }
    }


    @Override
    public Investor findById(String id) {
        Document doc = collection.find(new Document("_id", id)).first();
        return doc != null ? documentToInvestor(doc) : null;
    }

    private Investor documentToInvestor(Document doc) {
        // convert MongoDB Document to JSONObject
        try {
            return new Investor(new JSONObject(doc.toJson()));
        } catch (Exception e) {
            System.out.println("Error converting Document to JSONObject: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<Investor> findAll() {
        try {
            List<Investor> result = new ArrayList<>();
            collection.find().forEach(doc -> {
                Investor investor = documentToInvestor((Document) doc);
                result.add(investor);
            });
            return result;
        } catch (Exception e) {
            System.out.println("Error finding all investors: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    @Override
    public void save(Investor investor) {
        // We should ensure not already exists but indexes do that for us
        try {
            JSONObject json = investor.toJson();
            Document doc = new Document(json.toMap());  // No type loss using toMap
            collection.insertOne(doc);
            investor.setInvestorId(doc.getObjectId("_id"));
        } catch (Exception e) {
            System.out.println("Error saving Investor: " + e.getMessage());
            throw new RuntimeException("Error saving investor: " + e.getMessage());
        }
    }

    @Override
    public void update(Investor majInvestor) {
       try {
           // convert the updated investor to JSON string
           String jsonString = majInvestor.toString();
           // create a MongoDB Document from the JSON string
           Document doc = Document.parse(jsonString);
           // now we update the document in mongo
           collection.updateOne(new Document("_id", majInvestor.getInvestorId()), new Document("$set", doc));
       } catch (Exception e) {
              System.out.println("Error updating Investor: " + e.getMessage());
           throw new RuntimeException("Error updating investor: " + e.getMessage());
       }
    }

    @Override
    public void deleteById(String id) {
        try {
            collection.deleteOne(new Document("_id", id));
        } catch (Exception e) {
            System.out.println("Error deleting Investor: " + e.getMessage());
            throw new RuntimeException("Error deleting investor: " + e.getMessage());
        }
    }
}
