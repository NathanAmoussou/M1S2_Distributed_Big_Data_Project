package DAO;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.Document;
import Models.Transaction;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransactionDAO implements GenericDAO<Transaction> {
    private final MongoCollection<Document> collection;
    public TransactionDAO(MongoDatabase database) {
        this.collection = database.getCollection("transactions");
    }

    // findById method to find a transaction by its ID without session
    @Override
    public Transaction findById(String id) {
        return findById(null, id); // Call session-aware version
    }

    // findById with session
    public Transaction findById(ClientSession session, String id) {
        Document filter = new Document("_id", new ObjectId(id));
        Document doc = (session != null)
                ? collection.find(session, filter).first()
                : collection.find(filter).first();
        return doc != null ? documentToTransaction(doc) : null;
    }

    private Transaction documentToTransaction(Document doc) {
        try{
            Transaction transaction = new Transaction(new JSONObject(doc.toJson()));
            return transaction;
        } catch (Exception e) {
            System.out.println("Error converting document to Transaction: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public List<Transaction> findAll() { // we should not use as if all lot of transactions not optimal or maybe we can implement a paginated version if needed
       List<Transaction> transactions = new ArrayList<>();
        for (Document doc : collection.find()) {
            transactions.add(documentToTransaction(doc));
        }
        return transactions;
    }

    // save method to save a transaction without session
    @Override
    public void save(Transaction transaction) {
        save(null, transaction); // Call session-aware version
    }

    // save with session
    public void save(ClientSession session, Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction to save cannot be null");
        }
        // check if createdAt or updatedAt is null and set it to now
        if (transaction.getCreatedAt() == null) {
            transaction.setCreatedAt(LocalDateTime.now());
        }
        if (transaction.getUpdatedAt() == null) {
            transaction.setUpdatedAt(LocalDateTime.now());
        }
        try {
            JSONObject json = transaction.toJson();
            Document doc = new Document(json.toMap());
            if (session != null) {
                collection.insertOne(session, doc);
            } else {
                collection.insertOne(doc);
            }
        } catch (Exception e) {
//            System.out.println("Error saving transaction: " + e.getMessage());
            throw new RuntimeException("Error saving transaction: " + e.getMessage());
        }
    }

    // update method to update a transaction without session
    @Override
    public void update(Transaction transaction) {
        update(null, transaction); // Call session-aware version
    }

    // update with session
    public void update(ClientSession session, Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction to update cannot be null");
        }
        try {
            JSONObject json = transaction.toJson();
            Document doc = new Document(json.toMap());
//            System.out.println("Updating transaction: " + transaction);
            Document filter = new Document("_id", transaction.getTransactionId());
            Document updateDoc = new Document("$set", doc);
            updateDoc.get("$set", Document.class).remove("_id"); // Remove _id from update as it should not be updated
            if (session != null) {
                collection.updateOne(session, filter, updateDoc);
            } else {
                collection.updateOne(filter, updateDoc);
            }
        } catch (Exception e) {
//            System.out.println("Error updating transaction: " + e.getMessage());
            throw new RuntimeException("Error updating transaction: " + e.getMessage());
        }


    }

    @Override
    public void deleteById(String id) {
        try {
            collection.deleteOne(new Document("_id", new ObjectId(id)));
        } catch (Exception e) {
            System.out.println("Error deleting transaction: " + e.getMessage());
        }
    }

    public List<Transaction> getTransactionsByWalletId(ObjectId walletId) {
        List<Transaction> result = new ArrayList<>();
        for (Document doc : collection.find(new Document("walletId", walletId))) {
            result.add(documentToTransaction(doc));
        }
        return result;
    }

    public List<Transaction> getTransactionsByStockId(String stockTicker) {
        List<Transaction> result = new ArrayList<>();
        for (Document doc : collection.find(new Document("stockId", stockTicker))) {
            result.add(documentToTransaction(doc));
        }
        return result;
    }

    public List<Transaction> findByWalletIdAndDateRange(ObjectId walletId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<Transaction> results = new ArrayList<>();
        try {
            List<Bson> filters = new ArrayList<>();
            filters.add(Filters.eq("walletId", walletId));
            if (startDateTime != null) {
                filters.add(Filters.gte("createdAt", startDateTime));
            }
            if (endDateTime != null) {
                filters.add(Filters.lte("createdAt", endDateTime));
            }
            Bson finalFilter = Filters.and(filters);

            collection.find(finalFilter)
                    .sort(Sorts.descending("createdAt")) // Show newest first
                    .forEach(doc -> {
                        Transaction t = documentToTransaction(doc);
                        if (t != null) results.add(t);
                    });
            return results;
        } catch (Exception e) {
            System.err.println("Error finding transactions by investorId and date range: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Document> aggregateStockTransactionCounts(LocalDateTime startDateTime, LocalDateTime endDateTime, int limit) {
        List<Document> results = new ArrayList<>();
        try {
            List<Bson> pipeline = new ArrayList<>();

            // 1. Match transactions within the date range
            List<Bson> matchFilters = new ArrayList<>();
            if (startDateTime != null) {
                matchFilters.add(Filters.gte("createdAt", startDateTime));
            }
            if (endDateTime != null) {
                matchFilters.add(Filters.lte("createdAt", endDateTime));
            }
            if (!matchFilters.isEmpty()){
                pipeline.add(Aggregates.match(Filters.and(matchFilters)));
            }

            // 2. Group by stockTicker and count transactions
            pipeline.add(Aggregates.group("$stockId", Accumulators.sum("transactionCount", 1)));

            // 3. Sort by count descending
            pipeline.add(Aggregates.sort(Sorts.descending("transactionCount")));

            // 4. Limit the results
            pipeline.add(Aggregates.limit(limit));

            // 5. Project to rename _id to stockTicker for cleaner output (optional)
            pipeline.add(Aggregates.project(
                    Projections.fields(
                            Projections.excludeId(),
                            Projections.computed("stockTicker", "$_id"),
                            Projections.include("transactionCount")
                    )
            ));

            collection.aggregate(pipeline).forEach(results::add);
            return results;

        } catch (Exception e) {
            System.err.println("Error aggregating stock transaction counts: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Transaction> findTransactionsByWalletAndStock(ObjectId walletId, String stockTicker) {
        List<Transaction> results = new ArrayList<>();
        try {
            Document filter = new Document("walletId", walletId).append("stockId", stockTicker);
            collection.find(filter)
                    .sort(Sorts.ascending("createdAt")) // Optional: sort by date
                    .forEach(doc -> {
                        Transaction t = documentToTransaction(doc);
                        if (t != null) results.add(t);
                    });
            return results;
        } catch (Exception e) {
            System.err.println("Error finding transactions by walletId and stockTicker: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(); // Return empty list on error
        }
    }

    /**
     * Calculates the total amount spent on buys and received from sells for a specific wallet
     * using MongoDB aggregation.
     *
     * @param walletId The ObjectId of the wallet.
     * @return A Document containing "totalSpentOnBuys" and "totalReceivedFromSells" as BigDecimal,
     *         or null if an error occurs or no transactions found.
     */
    public Document aggregateBuySellTotals(ObjectId walletId) {
        List<Bson> pipeline = Arrays.asList(
                // Match transactions for the specific wallet
                Aggregates.match(Filters.eq("walletId", walletId)),

                // Calculate transaction value (quantity * priceAtTransaction)
                Aggregates.addFields(new Field<>("transactionValue",
                        new Document("$multiply", Arrays.asList("$quantity", "$priceAtTransaction"))
                )),

                // Group by transaction type (BUY/SELL) and sum the transaction values
                Aggregates.group(
                        "$transactionTypesId", // Group by the type field
                        Accumulators.sum("totalValue", "$transactionValue")
                ),

                // Group again (without an _id) to get the results into a single document
                Aggregates.group(
                        null, // Group all results into one document
                        Accumulators.push("totalsByType", // Create an array of { _id: type, totalValue: value }
                                new Document("type", "$_id").append("total", "$totalValue")
                        )
                ),

                // project to reshape the output into the desired format
                Aggregates.project(Projections.fields(
                        Projections.excludeId(), // Exclude the default _id from the final group stage
                        Projections.computed("totalSpentOnBuys",
                                // Use $reduce to find the total for "BUY" type in the array
                                new Document("$reduce", new Document("input", "$totalsByType")
                                        .append("initialValue", BigDecimal.ZERO) // Start with Decimal128 zero
                                        .append("in", new Document("$cond", Arrays.asList(
                                                new Document("$eq", Arrays.asList("$$this.type", "BUY")), // If type is "BUY"
                                                new Document("$add", Arrays.asList("$$value", "$$this.total")), // Add its total
                                                "$$value" // Otherwise, keep the accumulator the same
                                        )))
                                )
                        ),
                        Projections.computed("totalReceivedFromSells",
                                // Use $reduce to find the total for "SELL" type in the array
                                new Document("$reduce", new Document("input", "$totalsByType")
                                        .append("initialValue", BigDecimal.ZERO)
                                        .append("in", new Document("$cond", Arrays.asList(
                                                new Document("$eq", Arrays.asList("$$this.type", "SELL")), // If type is "SELL"
                                                new Document("$add", Arrays.asList("$$value", "$$this.total")),
                                                "$$value"
                                        )))
                                )
                        )
                ))
        );

        try {
            // Execute the aggregation pipeline
            Document result = collection.aggregate(pipeline).first();

            // Process the result to return BigDecimals
            if (result != null) {
                Document processedResult = new Document();
                Object spentObj = result.get("totalSpentOnBuys");
                Object receivedObj = result.get("totalReceivedFromSells");

                processedResult.put("totalSpentOnBuys", convertToBigDecimal(spentObj));
                processedResult.put("totalReceivedFromSells", convertToBigDecimal(receivedObj));
                return processedResult;

            } else {
                // No transactions found, return zero totals
                return new Document("totalSpentOnBuys", BigDecimal.ZERO)
                        .append("totalReceivedFromSells", BigDecimal.ZERO);
            }
        } catch (Exception e) {
            System.err.println("Error aggregating buy/sell totals for wallet " + walletId + ": " + e.getMessage());
            e.printStackTrace();
            return null; // Indicate error
        }
    }

    /**
     * Aggregates the total amount spent on buys and received from sells
     * for a specific stock within a specific wallet.
     *
     * @param walletId    The ObjectId of the wallet.
     * @param stockTicker The ticker symbol of the stock.
     * @return A Document containing "totalSpentOnBuys" and "totalReceivedFromSells",
     *         or a document with zeros if no transactions or an error occurs.
     */
    public Document aggregateBuySellTotalsForStock(ObjectId walletId, String stockTicker) {
        List<Bson> pipeline = Arrays.asList(
                // We match transactions for the specific wallet and stock
                Aggregates.match(Filters.and(
                        Filters.eq("walletId", walletId),
                        Filters.eq("stockId", stockTicker)
                )),

                // Calculate transaction value
                Aggregates.addFields(new Field<>("transactionValue",
                        new Document("$multiply", Arrays.asList("$quantity", "$priceAtTransaction"))
                )),

                // Group by transaction type BUY OR SELL and sum values
                Aggregates.group(
                        "$transactionTypesId",
                        Accumulators.sum("totalValue", "$transactionValue")
                ),

                // Group again to put the results into a single document
                Aggregates.group(
                        null,
                        Accumulators.push("totalsByType",
                                new Document("type", "$_id").append("total", "$totalValue")
                        )
                ),

                // Project to get the final structure with BigDecimal.ZERO as default
                Aggregates.project(Projections.fields(
                        Projections.excludeId(),
                        Projections.computed("totalSpentOnBuys",
                                new Document("$reduce", new Document("input", "$totalsByType")
                                        .append("initialValue", BigDecimal.ZERO)
                                        .append("in", new Document("$cond", Arrays.asList(
                                                new Document("$eq", Arrays.asList("$$this.type", "BUY")),
                                                new Document("$add", Arrays.asList("$$value", "$$this.total")),
                                                "$$value"
                                        )))
                                )
                        ),
                        Projections.computed("totalReceivedFromSells",
                                new Document("$reduce", new Document("input", "$totalsByType")
                                        .append("initialValue", BigDecimal.ZERO)
                                        .append("in", new Document("$cond", Arrays.asList(
                                                new Document("$eq", Arrays.asList("$$this.type", "SELL")),
                                                new Document("$add", Arrays.asList("$$value", "$$this.total")),
                                                "$$value"
                                        )))
                                )
                        )
                ))
        );

        try {
            Document result = collection.aggregate(pipeline).first();

            if (result != null) {
                // We ensure results are in BigDecimals
                Document processedResult = new Document();
                processedResult.put("totalSpentOnBuys", convertToBigDecimal(result.get("totalSpentOnBuys")));
                processedResult.put("totalReceivedFromSells", convertToBigDecimal(result.get("totalReceivedFromSells")));
                return processedResult;
            } else {
                // if no matching transactions found wee rerutin zeros
                return new Document("totalSpentOnBuys", BigDecimal.ZERO)
                        .append("totalReceivedFromSells", BigDecimal.ZERO);
            }
        } catch (Exception e) {
            System.err.println("Error aggregating buy/sell totals for wallet " + walletId + " and stock " + stockTicker + ": " + e.getMessage());
            e.printStackTrace();
            // we  return zeros on error to avoid breaking calculations in service layer
            return new Document("totalSpentOnBuys", BigDecimal.ZERO)
                    .append("totalReceivedFromSells", BigDecimal.ZERO);
        }
    }


    private BigDecimal convertToBigDecimal(Object value) {
        if (value instanceof Decimal128) {
            return ((Decimal128) value).bigDecimalValue();
        } else if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return BigDecimal.ZERO; // Default to zero if null or unexpected type
    }


}

