package DAO;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import org.bson.Document;
import Models.Transaction;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO implements GenericDAO<Transaction> {
    private final MongoCollection<Document> collection;
    public TransactionDAO(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public Transaction findById(String id) {
        Document doc = collection.find(new Document("_id", id)).first();
        return doc != null ? documentToTransaction(doc) : null;
    }

    private Transaction documentToTransaction(Document doc) {
        try{
            Transaction transaction = new Transaction(new JSONObject(doc.toJson()));
            return transaction;
        } catch (Exception e) {
            System.out.println("Error converting document to Transaction: " + e.getMessage());
            return null;
        }

    }

    @Override
    public List<Transaction> findAll() { // WE SHOULD NOT USE FIND ALL AS WE ARE IN BIG DATA AND A LOT OF DOCS
       List<Transaction> transactions = new ArrayList<>();
        for (Document doc : collection.find()) {
            transactions.add(documentToTransaction(doc));
        }
        return transactions;
    }

    @Override
    public void save(Transaction transaction) {
        try {
            JSONObject json = transaction.toJson();
            Document doc = new Document(json.toMap());
            collection.insertOne(doc);
        } catch (Exception e) {
            System.out.println("Error saving transaction: " + e.getMessage());
        }
    }

    @Override
    public void update(Transaction transaction) {
        try {
            JSONObject json = transaction.toJson();
            Document doc = new Document(json.toMap());
            System.out.println("Updating transaction: " + transaction);
            collection.updateOne(new Document("_id", transaction.getTransactionId()), new Document("$set", doc));
        } catch (Exception e) {
            System.out.println("Error updating transaction: " + e.getMessage());
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
}
