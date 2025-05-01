package DAO;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import Models.Wallet;
import org.bson.Document;
import Models.Investor;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.time.LocalDateTime;
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

            // Creating a unique index on each wallet's walletId
            collection.createIndex(Indexes.ascending("wallets.walletId"), new IndexOptions().unique(true));

            System.out.println("Unique indexes on 'username', 'email', and 'wallets.walletId' created successfully.");
        } catch (Exception e) {
            System.err.println("Error creating indexes: " + e.getMessage());
        }
    }


    @Override
    public Investor findById(String id) {
        try {
            System.out.println("Find Investor by ID: " + id);
            Document doc = collection.find(new Document("_id", new ObjectId(id))).first();
            return doc != null ? documentToInvestor(doc) : null;
        } catch (IllegalArgumentException e) {
           throw new IllegalArgumentException("Invalid ID format: " + id, e);
        } catch (Exception e) {
            throw new RuntimeException("Error finding investor by ID: " + e.getMessage());
        }

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
    public List<Investor> findAll() { // SHOULD NOT USE IT !!!!!!!!!
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
           JSONObject json = majInvestor.toJson();
           Document doc = new Document(json.toMap());  // No type loss using toMap
           // now we update the document in mongo
           System.out.println("Updating Investor: " + majInvestor);
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

    public Investor findInvestorByWalletId(String walletId) {
        try {
            ObjectId walletObjectId = new ObjectId(walletId);
            Document doc = collection.find(new Document("wallets.walletId", walletObjectId)).first();
            if (doc != null) {
                System.out.println("Found investor: " + doc.toJson());
                Investor investor = documentToInvestor(doc);
//                System.out.println("Investor found: " + investor);
                return investor;
            } else {
                System.out.println("No investor found with walletId: " + walletId);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid walletId format: " + walletId);
        }
        return null;
    }

    public Investor findByEmail(String email) {
        try {
            System.out.println("Find Investor by Email: " + email);
            // as it is unique we can use first
            Document doc = collection.find(Filters.eq("email", email)).first();
            return doc != null ? documentToInvestor(doc) : null;
        } catch (Exception e) {
            System.err.println("Error finding investor by email: " + e.getMessage());
            return null;
        }
    }

    public Investor findByUsername(String username) {
        try {
            System.out.println("Find Investor by Username: " + username);
            // as it is unique we can use first
            Document doc = collection.find(Filters.eq("username", username)).first();
            return doc != null ? documentToInvestor(doc) : null;
        } catch (Exception e) {
            System.err.println("Error finding investor by username: " + e.getMessage());
            return null;
        }
    }

    public Wallet getWalletById(String walletId) {
        try {
            ObjectId walletObjectId = new ObjectId(walletId);
            Document doc = collection.find(new Document("wallets.walletId", walletObjectId)).first();
            if (doc != null) {
                List<Document> wallets = (List<Document>) doc.get("wallets");
                for (Document walletDoc : wallets) {
                    if (walletDoc.getObjectId("walletId").equals(walletObjectId)) {
                        return new Wallet(new JSONObject(walletDoc.toJson()));
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("cannot get a wallet for walletId : " + walletId + e.getMessage());
        }
        return null;
    }

    public Wallet updateWallet(Wallet wallet) {
        try {
            Document doc = new Document("wallets.walletId", wallet.getWalletId());
            Document updateDoc = new Document("$set", new Document("wallets.$", new Document(wallet.toJson().toMap())));
            collection.updateOne(doc, updateDoc);
//            return findInvestorByWalletId(wallet.getWalletId().toString());
            return wallet;
        } catch (Exception e) {
            System.out.println("Error updating Wallet: " + e.getMessage());
            throw new RuntimeException("Error updating wallet: " + e.getMessage());
        }
    }


    public boolean updateInvestorPartial(String investorId, Document updates) {
        try {
            if (!ObjectId.isValid(investorId)) {
                throw new IllegalArgumentException("Invalid Investor ID format for partial update: " + investorId);
            }
            if (updates == null || updates.isEmpty()) {
                System.out.println("No updates provided for investor " + investorId);
                return false; // Or throw exception?
            }

            // Ensure we don't try to update _id or arrays directly with this method
            updates.remove("_id");
            updates.remove("wallets"); // wallets should be updated with updateWallet
            updates.remove("addresses"); // addresses should be updated with updateAddress
            updates.remove("creationDate"); // Should not be updated

            if (updates.isEmpty()) {
                System.out.println("No valid fields left to update for investor " + investorId);
                return false;
            }

            // Add lastUpdateDate automatically
            updates.put("lastUpdateDate", LocalDateTime.now());

            Document filter = new Document("_id", new ObjectId(investorId));
            Document updateOperation = new Document("$set", updates);

            com.mongodb.client.result.UpdateResult result = collection.updateOne(filter, updateOperation);

            if (result.getMatchedCount() == 0) {
                System.err.println("Investor not found for partial update: " + investorId);
                return false;
            }
            System.out.println("Partially updated investor " + investorId + ". Matched: " + result.getMatchedCount() + ", Modified: " + result.getModifiedCount());
            return result.getModifiedCount() > 0 || result.getMatchedCount() > 0; // Return true if matched, even if no fields changed value

        } catch (IllegalArgumentException e) {
            throw e; // Propagate validation error
        } catch (Exception e) {
            System.err.println("Error partially updating investor " + investorId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
