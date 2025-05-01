package Utils;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ValidationOptions;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class to manage MongoDB indexes and schema validators for the gestionBourse database.
 * This class ensures proper database configuration, including indexes for performance optimization
 * and schema validation for data integrity.
 */
public class DatabaseSetupManager {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "gestionBourse";

    public static void main(String[] args) {
        System.out.println("Starting Database Setup Manager...");

        try (MongoClient mongoClient = MongoClients.create(MONGO_URI)) {
            MongoDatabase database = mongoClient.getDatabase(DB_NAME);

            System.out.println("Connected to database: " + DB_NAME);

            // Setup indexes for all collections
            setupInvestorsIndexes(database);
            setupStocksIndexes(database);
            setupTransactionsIndexes(database);
            setupHoldingsIndexes(database);

            // Setup schema validators for all collections
            setupInvestorsValidator(database);
            setupStocksValidator(database);
            setupTransactionsValidator(database);
            setupHoldingsValidator(database);

            System.out.println("Database setup completed successfully.");
        } catch (Exception e) {
            System.err.println("Error during database setup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ====== INDEX MANAGEMENT METHODS ======

    private static void setupInvestorsIndexes(MongoDatabase database) {
        MongoCollection<Document> collection = database.getCollection("investors");
        System.out.println("\nChecking indexes for 'investors' collection:");

        // Username index (unique)
        createIndexIfNotExists(collection, Indexes.ascending("username"), true);

        // Email index (unique)
        createIndexIfNotExists(collection, Indexes.ascending("email"), true);
    }

    private static void setupStocksIndexes(MongoDatabase database) {
        MongoCollection<Document> collection = database.getCollection("stocks");
        System.out.println("\nChecking indexes for 'stocks' collection:");

        // StockTicker index (unique)
        createIndexIfNotExists(collection, Indexes.ascending("stockTicker"), true);
    }

    private static void setupTransactionsIndexes(MongoDatabase database) {
        MongoCollection<Document> collection = database.getCollection("transactions");
        System.out.println("\nChecking indexes for 'transactions' collection:");

        // WalletId index
        createIndexIfNotExists(collection, Indexes.ascending("walletId"), false);

        // StockId index
        createIndexIfNotExists(collection, Indexes.ascending("stockId"), false);

        // CreatedAt index (descending for recent first)
        createIndexIfNotExists(collection, Indexes.descending("createdAt"), false);
    }

    private static void setupHoldingsIndexes(MongoDatabase database) {
        MongoCollection<Document> collection = database.getCollection("holdings");
        System.out.println("\nChecking indexes for 'holdings' collection:");

        // WalletId index
        createIndexIfNotExists(collection, Indexes.ascending("walletId"), false);

        // StockTicker index
        createIndexIfNotExists(collection, Indexes.ascending("stockTicker"), false);

        // Compound index for wallet+stockTicker (unique)
        createIndexIfNotExists(collection,
                Indexes.compoundIndex(Indexes.ascending("walletId"), Indexes.ascending("stockTicker")),
                true);
    }

    /**
     * Creates an index if it doesn't already exist on the collection.
     */
    private static void createIndexIfNotExists(MongoCollection<Document> collection,
                                               org.bson.conversions.Bson indexKeys,
                                               boolean unique) {
        try {
            // Extract the index name from the keys
            Document keysDoc = indexKeys.toBsonDocument(Document.class,
                                       MongoClient.getDefaultCodecRegistry());
            String indexName = keysDoc.keySet().iterator().next(); // Get first field name

            // Check if index exists
            boolean indexExists = false;
            FindIterable<Document> existingIndexes = collection.listIndexes();
            for (Document index : existingIndexes) {
                Document keyDoc = (Document) index.get("key");
                if (keyDoc != null && keyDoc.containsKey(indexName)) {
                    indexExists = true;
                    System.out.println("  ✓ Index already exists for field '" + indexName +
                                        "' on collection " + collection.getNamespace().getCollectionName());
                    break;
                }
            }

            if (!indexExists) {
                IndexOptions options = new IndexOptions();
                if (unique) {
                    options.unique(true);
                }
                collection.createIndex(indexKeys, options);
                System.out.println("  + Created " + (unique ? "unique " : "") + "index for field '" +
                                   indexName + "' on collection " +
                                   collection.getNamespace().getCollectionName());
            }
        } catch (Exception e) {
            System.err.println("  ! Error creating index: " + e.getMessage());
        }
    }

    // ====== SCHEMA VALIDATOR METHODS ======

    private static void setupInvestorsValidator(MongoDatabase database) {
        System.out.println("\nChecking schema validator for 'investors' collection:");

        // Create validator schema for investors
        Document investorsSchema = new Document("$jsonSchema", new Document()
            .append("bsonType", "object")
            .append("required", Arrays.asList(
                "username", "password", "name", "surname", "email", "phoneNumber",
                "addresses", "wallets"))
            .append("properties", new Document()
                .append("username", new Document("bsonType", "string"))
                .append("password", new Document("bsonType", "string"))
                .append("name", new Document("bsonType", "string"))
                .append("surname", new Document("bsonType", "string"))
                .append("email", new Document("bsonType", "string"))
                .append("phoneNumber", new Document("bsonType", "string"))
                .append("creationDate", new Document("bsonType", "date"))
                .append("lastUpdateDate", new Document("bsonType", "date"))
                .append("addresses", new Document()
                    .append("bsonType", "array")
                    .append("minItems", 1)
                    .append("items", new Document()
                        .append("bsonType", "object")
                        .append("required", Arrays.asList("street", "zipCode", "city", "country"))
                        .append("properties", new Document()
                            .append("addressId", new Document("bsonType", "objectId"))
                            .append("number", new Document("bsonType", "string"))
                            .append("street", new Document("bsonType", "string"))
                            .append("zipCode", new Document("bsonType", "string"))
                            .append("city", new Document("bsonType", "string"))
                            .append("country", new Document("bsonType", "string"))
                        )
                    )
                )
                .append("wallets", new Document()
                    .append("bsonType", "array")
                    .append("minItems", 1)
                    .append("items", new Document()
                        .append("bsonType", "object")
                        .append("required", Arrays.asList("walletId", "currencyCode", "balance", "walletType"))
                        .append("properties", new Document()
                            .append("walletId", new Document("bsonType", "objectId"))
                            .append("currencyCode", new Document("bsonType", "string"))
                            .append("balance", new Document("bsonType", ["decimal", "double", "int", "long"]))
                            .append("walletType", new Document("bsonType", "string"))
                        )
                    )
                )
            )
        );

        applyValidatorIfNotExists(database, "investors", investorsSchema);
    }

    private static void setupStocksValidator(MongoDatabase database) {
        System.out.println("\nChecking schema validator for 'stocks' collection:");

        // Create validator schema for stocks
        Document stocksSchema = new Document("$jsonSchema", new Document()
            .append("bsonType", "object")
            .append("required", Arrays.asList("stockTicker", "stockName", "lastPrice"))
            .append("properties", new Document()
                .append("stockTicker", new Document("bsonType", "string"))
                .append("stockName", new Document("bsonType", "string"))
                .append("market", new Document("bsonType", "string"))
                .append("industry", new Document("bsonType", "string"))
                .append("sector", new Document("bsonType", "string"))
                .append("lastPrice", new Document("bsonType", ["decimal", "double", "string"]))
                .append("lastUpdated", new Document("bsonType", ["date", "string"]))
                .append("country", new Document("bsonType", "string"))
                .append("currency", new Document("bsonType", "string"))
            )
        );

        applyValidatorIfNotExists(database, "stocks", stocksSchema);
    }

    private static void setupTransactionsValidator(MongoDatabase database) {
        System.out.println("\nChecking schema validator for 'transactions' collection:");

        // Create validator schema for transactions
        Document transactionsSchema = new Document("$jsonSchema", new Document()
            .append("bsonType", "object")
            .append("required", Arrays.asList(
                "quantity", "priceAtTransaction", "createdAt", "stockId", "walletId", "transactionTypesId"))
            .append("properties", new Document()
                .append("quantity", new Document("bsonType", ["decimal", "double"]))
                .append("priceAtTransaction", new Document("bsonType", ["decimal", "double"]))
                .append("createdAt", new Document("bsonType", ["date", "string"]))
                .append("updatedAt", new Document("bsonType", ["date", "string"]))
                .append("stockId", new Document("bsonType", "string"))
                .append("walletId", new Document("bsonType", "objectId"))
                .append("transactionTypesId", new Document("bsonType", "string"))
                .append("transactionStatusId", new Document("bsonType", "string"))
            )
        );

        applyValidatorIfNotExists(database, "transactions", transactionsSchema);
    }

    private static void setupHoldingsValidator(MongoDatabase database) {
        System.out.println("\nChecking schema validator for 'holdings' collection:");

        // Create validator schema for holdings
        Document holdingsSchema = new Document("$jsonSchema", new Document()
            .append("bsonType", "object")
            .append("required", Arrays.asList("walletId", "stockTicker", "quantity"))
            .append("properties", new Document()
                .append("walletId", new Document("bsonType", "objectId"))
                .append("stockTicker", new Document("bsonType", "string"))
                .append("quantity", new Document("bsonType", ["decimal", "double"]))
                .append("totalBuyCost", new Document("bsonType", ["decimal", "double"]))
                .append("totalSellCost", new Document("bsonType", ["decimal", "double"]))
                .append("lastUpdated", new Document("bsonType", ["date", "string"]))
            )
        );

        applyValidatorIfNotExists(database, "holdings", holdingsSchema);
    }

    /**
     * Applies a schema validator to a collection if it doesn't already have one.
     */
    private static void applyValidatorIfNotExists(MongoDatabase database, String collectionName, Document validator) {
        try {
            // Check if collection exists
            boolean collectionExists = false;
            for (String name : database.listCollectionNames()) {
                if (name.equals(collectionName)) {
                    collectionExists = true;
                    break;
                }
            }

            if (collectionExists) {
                // Check if validator exists
                Document collInfo = database.runCommand(
                    new Document("listCollections", 1)
                        .append("filter", new Document("name", collectionName))
                );

                Document firstBatch = (Document) ((Document) collInfo.get("cursor")).get("firstBatch");
                List<Document> batches = (List<Document>) firstBatch.get("0");

                boolean hasValidator = false;
                if (batches != null && !batches.isEmpty()) {
                    Document options = (Document) batches.get(0).get("options");
                    hasValidator = options != null && options.containsKey("validator");
                }

                if (!hasValidator) {
                    // Apply validator to existing collection
                    database.runCommand(
                        new Document("collMod", collectionName)
                            .append("validator", validator)
                            .append("validationLevel", "moderate") // moderate allows existing docs to remain valid
                            .append("validationAction", "error")
                    );
                    System.out.println("  + Applied schema validator to collection: " + collectionName);
                } else {
                    System.out.println("  ✓ Schema validator already exists for collection: " + collectionName);
                }
            } else {
                // Create new collection with validator
                database.createCollection(collectionName,
                    new ValidationOptions()
                        .validator(validator)
                        .validationLevel("moderate")
                        .validationAction("error")
                );
                System.out.println("  + Created collection '" + collectionName + "' with schema validator");
            }
        } catch (Exception e) {
            System.err.println("  ! Error setting up validator for collection '" + collectionName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }
}
