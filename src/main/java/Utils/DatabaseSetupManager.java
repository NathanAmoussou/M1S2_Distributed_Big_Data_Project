package Utils;

import com.mongodb.client.*;

import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.CreateCollectionOptions; // Needed for createCollection

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

    private static final String STANDALONE_MONGO_URI = "mongodb://localhost:27017";
    private static final String STANDALONE_DB_NAME = "gestionBourse";

    // *** ADD THIS NEW METHOD ***
    public static void runSetup(MongoDatabase database) {
        System.out.println("Starting Database Setup Manager logic...");
        // NO MongoClient creation here - use the passed 'database' object
        try {
            System.out.println("Using provided database connection: " + database.getName());

            // Setup indexes (calls remain the same, they already take database)
            setupInvestorsIndexes(database);
            setupStocksIndexes(database);
            setupTransactionsIndexes(database);
            setupHoldingsIndexes(database);
//             setupStockPriceHistoryIndexes(database);

            // Setup validators (calls remain the same)
            setupInvestorsValidator(database);
            setupStocksValidator(database);
            setupTransactionsValidator(database);
            setupHoldingsValidator(database);
//            setupStockPriceHistoryValidator(database);

            System.out.println("Database setup logic completed successfully.");
        } catch (Exception e) {
            System.err.println("Error during database setup logic: " + e.getMessage());
            throw new RuntimeException("Database setup failed", e);
        }
    }

    // for standalone testing
    public static void main(String[] args) {
        System.out.println("Starting Database Setup Manager (Standalone)...");
        try (MongoClient mongoClient = MongoClients.create(STANDALONE_MONGO_URI)) { // Connect
            MongoDatabase database = mongoClient.getDatabase(STANDALONE_DB_NAME); // Get DB
            runSetup(database); // <<<< CALL THE NEW METHOD
        } catch (Exception e) {
            System.err.println("Error during standalone database setup: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        // Client closed automatically by try-with-resources
    }


    // ====== INDEX MANAGEMENT METHODS ======

    private static void setupInvestorsIndexes(MongoDatabase database) {
        MongoCollection<Document> collection = database.getCollection("investors");
        System.out.println("\nChecking indexes for 'investors' collection:");

        // Username index (unique)
        createIndexIfNotExists(collection, Indexes.ascending("username"), true);

        // Email index (unique) -> NOT POSSIBLE WITH CLUSTER AND SHARD ONLY THE SHARD KEY CAN BE UNIQUE
//        createIndexIfNotExists(collection, Indexes.ascending("email"), true);
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
                                               org.bson.conversions.Bson indexKeys, // Garder Bson ici
                                               boolean unique) {
        String collectionName = collection.getNamespace().getCollectionName();
        String indexDescription = indexKeys.toString(); // Simple description for logging

        try {
            // Check if an index with the exact same definition (keys + options) already exists
            boolean indexExists = false;
            ListIndexesIterable<Document> existingIndexes = collection.listIndexes(); // Correct Type
            for (Document index : existingIndexes) {
                // Rebuild the Bson keys from the existing index document for potential comparison
                // Note: Comparing complex index options this way can be tricky.
                // A simpler approach is to just try creating and catch the potential duplicate error,
                // but we stick to the "check first" logic as requested.

                // Let's try a name-based check for simplicity for unique indexes,
                // and rely on createIndex not throwing for non-unique ones if identical.
                // For unique indexes, MongoDB often names them based on fields.

                // More robust check would involve parsing index.get("key") and comparing
                // field names and order, plus checking index.get("unique") etc.
                // For this project, we assume simple indexes and rely on createIndex's idempotency.

                // Simpler check: Just check if ANY index exists for the primary field.
                // This isn't perfect idempotency but prevents errors for basic cases.
                Document keyDoc = (Document) index.get("key");
                if (keyDoc != null) {
                    // A better check might be needed for compound keys if order matters
                    // or if multiple indexes start with the same field.
                    // For now, let's assume createIndex handles true idempotency
                    // if the exact same index exists.
                    // We log existing ones.
                    System.out.println("  Found existing index: " + index.toJson());

                }
            }

            // --- Attempt to create the index ---
            // MongoDB's createIndex is generally idempotent IF the index definition
            // (keys, options like unique, name) is EXACTLY the same.
            // It might be simpler to just call createIndex and log success/failure.

            IndexOptions options = new IndexOptions();
            String generatedIndexName = null; // Will store the name if created

            if (unique) {
                options.unique(true);
                // Optionally, try to force a name for unique indexes to help identification
                // options.name(generateIndexName(indexKeys, collection.getCodecRegistry()) + "_unique");
            }

            generatedIndexName = collection.createIndex(indexKeys, options);
            System.out.println("  ✓ Attempted creation for index: " + indexDescription + (unique ? " (unique)" : "") +
                    ". Resulting name (if created or already existed): " + generatedIndexName +
                    " on collection " + collectionName);
            indexExists = true; // Assume it exists now, either created or was already there.


        } catch (Exception e) { // Catch broader Exception, MongoCommandException for duplicate might occur
            // If createIndex fails because an index with the same *name* but *different* options exists,
            // or other issues occur.
            System.err.println("  ! Error during index creation/check for " + indexDescription +
                    " on collection " + collectionName + ": " + e.getMessage());
            // e.printStackTrace();
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
                            .append("balance", new Document("bsonType", Arrays.asList("decimal", "double", "int", "long")))
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
                .append("lastPrice", new Document("bsonType", Arrays.asList("decimal", "double", "string")))
                .append("lastUpdated", new Document("bsonType", Arrays.asList("date", "string")))
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
                .append("quantity", new Document("bsonType", Arrays.asList("decimal", "double")))
                .append("priceAtTransaction", new Document("bsonType", Arrays.asList("decimal", "double")))
                .append("createdAt", new Document("bsonType", Arrays.asList("date", "string")))
                .append("updatedAt", new Document("bsonType", Arrays.asList("date", "string")))
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
                .append("quantity", new Document("bsonType", Arrays.asList("decimal", "double")))
                .append("totalBuyCost", new Document("bsonType", Arrays.asList("decimal", "double")))
                .append("totalSellCost", new Document("bsonType", Arrays.asList("decimal", "double")))
                .append("lastUpdated", new Document("bsonType", Arrays.asList("date", "string")))
            )
        );

        applyValidatorIfNotExists(database, "holdings", holdingsSchema);
    }

    /**
     * Applies a schema validator to a collection if it doesn't already have one.
     */
    private static void applyValidatorIfNotExists(MongoDatabase database, String collectionName, Document validator) {
        try {
            // Check if collection exists and if it has a validator
            boolean collectionExists = false;
            boolean hasValidator = false;

            // Use listCollections command to get info including options
            MongoCursor<Document> cursor = database.listCollections()
                    .filter(new Document("name", collectionName))
                    .iterator();

            if (cursor.hasNext()) {
                collectionExists = true;
                Document collInfo = cursor.next(); // Get the document for our collection
                Document options = (Document) collInfo.get("options"); // Directly get the options document
                if (options != null && options.containsKey("validator")) {
                    // Optional: Compare existing validator with the new one for more robust updates
                    // Document existingValidator = (Document) options.get("validator");
                    // if(existingValidator.equals(validator)) { // Simple equality check might work
                    hasValidator = true;
                    System.out.println("  ✓ Schema validator already exists for collection: " + collectionName);
                    // } else {
                    //      System.out.println("  - Existing validator differs. Attempting update (collMod).");
                    // }
                }
            }
            cursor.close(); // Close the cursor

            if (!collectionExists) {
                System.out.println("  Collection '" + collectionName + "' does not exist. Creating with validator...");
                // Create new collection with validator using CreateCollectionOptions
                CreateCollectionOptions createOptions = new CreateCollectionOptions()
                        .validationOptions(new ValidationOptions()
                                .validator(validator)
                                .validationLevel(ValidationLevel.MODERATE) // Use enum directly
                                .validationAction(ValidationAction.ERROR)   // Use enum directly
                        );
                database.createCollection(collectionName, createOptions);
                System.out.println("  + Created collection '" + collectionName + "' with schema validator");

            } else if (!hasValidator) {
                System.out.println("  Collection '" + collectionName + "' exists but lacks validator. Applying (collMod)...");
                // Apply validator to existing collection
                database.runCommand(
                        new Document("collMod", collectionName)
                                .append("validator", validator)
                                .append("validationLevel", ValidationLevel.MODERATE.getValue()) // Use enum and getValue() for command
                                .append("validationAction", ValidationAction.ERROR.getValue())   // Use enum and getValue() for command
                );
                System.out.println("  + Applied schema validator to collection: " + collectionName);
            }
            // If collectionExists and hasValidator, we do nothing (already logged existence).

        } catch (Exception e) {
            System.err.println("   Error setting up validator for collection '" + collectionName + "': " + e.getMessage());
            // e.printStackTrace();
        }
    }
}
