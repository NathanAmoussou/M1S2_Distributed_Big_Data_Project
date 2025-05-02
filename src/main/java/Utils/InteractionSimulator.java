package Utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class to simulate user interactions with the trading system through API calls.
 * This populates the MongoDB collections with realistic transaction and holding data.
 *
 * Prerequisites:
 * 1. MongoDB running with populated investors collection (run DatabaseSeeder first)
 * 2. REST API running (app.Application) on localhost:8000
 * 3. Backend Python API running
 * 4. Some stock data available in the stocks collection
 */
public class InteractionSimulator {
    // Configuration
    private static final String STANDALONE_MONGO_URI = "mongodb://localhost:27017";
    private static final String STANDALONE_DB_NAME = "gestionBourse";
    private static final String API_BASE_URL = "http://localhost:8000";

    // Number of investors to process
    private static final int MAX_INVESTORS_TO_PROCESS = 50;

    // HTTP client for API calls
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Random generator for simulation
    private static final Random random = new Random();

    public static void runSimulation(MongoDatabase database) {
        System.out.println("========== Starting Interaction Simulator ==========");

        List<String> stockTickers = new ArrayList<>();
        List<InvestorWallet> investorWallets = new ArrayList<>();

        // Step 1: Connect to MongoDB and fetch initial data
        try {

            // Fetch stock tickers
            stockTickers = fetchStockTickers(database);
            if (stockTickers.isEmpty()) {
                System.err.println("ERROR: No stocks found in the database. Please ensure stocks are populated.");
                return;
            }
            System.out.println("✓ Fetched " + stockTickers.size() + " stock tickers from database.");
            System.out.println("  Sample tickers: " + stockTickers.subList(0, Math.min(5, stockTickers.size())));

            // Fetch investor and wallet IDs
            investorWallets = fetchInvestorWallets(database);
            if (investorWallets.isEmpty()) {
                System.err.println("ERROR: No investors found in the database. Please run DatabaseSeeder first.");
                return;
            }
            System.out.println("✓ Fetched " + investorWallets.size() + " investors from database.");

        } catch (Exception e) {
            System.err.println("ERROR connecting to MongoDB: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Step 2: Simulate interactions for each investor
        System.out.println("\n========== Beginning Simulation of User Interactions ==========");
        int successfulInvestors = 0;

        for (InvestorWallet investorWallet : investorWallets) {
            boolean success = simulateInvestorActions(investorWallet, stockTickers);
            if (success) successfulInvestors++;

            // Small delay between investors to prevent overwhelming the server
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("\n========== Interaction Simulation Completed ==========");
        System.out.println("Successfully processed " + successfulInvestors + " out of " +
                           investorWallets.size() + " investors.");
    }


    // *** MODIFY EXISTING main (Optional, for standalone testing) ***
    public static void main(String[] args) {
        System.out.println("========== Starting Interaction Simulator (Standalone) ==========");
        try (MongoClient mongoClient = MongoClients.create(STANDALONE_MONGO_URI)) { // Connect
            MongoDatabase database = mongoClient.getDatabase(STANDALONE_DB_NAME); // Get DB
            runSimulation(database);
        } catch (Exception e) {
            System.err.println("Error during standalone simulation: " + e.getMessage());
            e.printStackTrace();
        }
        // Client closed automatically by try-with-resources
    }

    /**
     * Fetches stock tickers from the stocks collection
     */
    private static List<String> fetchStockTickers(MongoDatabase database) {
        List<String> tickers = new ArrayList<>();
        try {
            MongoCursor<Document> cursor = database.getCollection("stocks")
                    .find()
                    .projection(new Document("stockTicker", 1).append("_id", 0))
                    .iterator();

            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String ticker = doc.getString("stockTicker");
                if (ticker != null && !ticker.isEmpty()) {
                    tickers.add(ticker);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching stock tickers: " + e.getMessage());
        }
        return tickers;
    }

    /**
     * Fetches a subset of investors with their wallet IDs
     */
    private static List<InvestorWallet> fetchInvestorWallets(MongoDatabase database) {
        List<InvestorWallet> investorWallets = new ArrayList<>();
        try {
            // Count total investors
            long totalInvestors = database.getCollection("investors").countDocuments();
            int limit = (int) Math.min(MAX_INVESTORS_TO_PROCESS, totalInvestors);

            MongoCursor<Document> cursor = database.getCollection("investors")
                    .find()
                    .limit(limit)
                    .iterator();

            while (cursor.hasNext()) {
                Document investor = cursor.next();
                ObjectId investorObjId = investor.getObjectId("_id");
                String investorId = investorObjId.toString();

                // Get the first wallet from the wallets array
                List<Document> wallets = (List<Document>) investor.get("wallets");
                if (wallets != null && !wallets.isEmpty()) {
                    Document wallet = wallets.get(0);
                    ObjectId walletObjId = wallet.getObjectId("walletId");
                    String walletId = walletObjId.toString();

                    // Add to our list
                    investorWallets.add(new InvestorWallet(investorId, walletId));
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching investors: " + e.getMessage());
        }
        return investorWallets;
    }

    /**
     * Simulates a series of actions for a single investor
     * @return true if the simulation was successful overall
     */
    private static boolean simulateInvestorActions(InvestorWallet investorWallet, List<String> stockTickers) {
        System.out.println("\n----- Processing investor: " + investorWallet.investorId +
                           " with wallet: " + investorWallet.walletId + " -----");

        try {
            // 1. Add funds to wallet
            BigDecimal amount = generateRandomAmount(500.0, 5000.0);
            boolean fundingSuccess = addFundsToWallet(investorWallet.walletId, amount);

            if (!fundingSuccess) {
                System.out.println("  Skipping further actions due to funding failure.");
                return false;
            }

            // 2. Buy stocks (2-5 times)
            int buyCount = random.nextInt(4) + 2; // 2 to 5
            int successfulBuys = 0;

            System.out.println("  Attempting " + buyCount + " stock purchases...");
            for (int i = 0; i < buyCount; i++) {
                String randomTicker = getRandomStockTicker(stockTickers);
                BigDecimal quantity = new BigDecimal(random.nextInt(10) + 1); // 1 to 10

                boolean buySuccess = buyStock(investorWallet.walletId, randomTicker, quantity);
                if (buySuccess) successfulBuys++;

                // Short delay between API calls
                Thread.sleep(500);
            }

            // 3. Sell stocks (1-2 times)
            if (successfulBuys > 0) {
                int sellCount = random.nextInt(2) + 1; // 1 to 2
                int successfulSells = 0;

                System.out.println("  Attempting " + sellCount + " stock sales...");
                for (int i = 0; i < sellCount; i++) {
                    // For selling, preferentially use tickers we might have bought
                    String randomTicker = getRandomStockTicker(stockTickers);
                    BigDecimal quantity = new BigDecimal(random.nextInt(3) + 1); // 1 to 3

                    boolean sellSuccess = sellStock(investorWallet.walletId, randomTicker, quantity);
                    if (sellSuccess) successfulSells++;

                    // Short delay between API calls
                    Thread.sleep(500);
                }

                System.out.println("  Completed with " + successfulBuys + "/" + buyCount +
                                   " successful buys and " + successfulSells + "/" +
                                   sellCount + " successful sells.");
            }

            return true;

        } catch (Exception e) {
            System.err.println("ERROR during simulation for investor " +
                               investorWallet.investorId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Adds funds to a wallet via API call
     */
    private static boolean addFundsToWallet(String walletId, BigDecimal amount) {
        try {
            System.out.println("  Adding funds: $" + amount + " to wallet: " + walletId);

            JSONObject requestBody = new JSONObject();
            requestBody.put("amount", amount);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/wallets/" + walletId + "/funds"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean success = (response.statusCode() >= 200 && response.statusCode() < 300);
            System.out.println("    Result: " + (success ? "SUCCESS" : "FAILED") +
                               " (Status: " + response.statusCode() + ")");

            if (!success) {
                System.out.println("    Error details: " + response.body());
            }

            return success;
        } catch (IOException | InterruptedException e) {
            System.err.println("    Error adding funds: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    /**
     * Buys stock via API call
     */
    private static boolean buyStock(String walletId, String stockTicker, BigDecimal quantity) {
        try {
            System.out.println("    Buying stock: " + stockTicker + ", Quantity: " + quantity);

            JSONObject requestBody = new JSONObject();
            requestBody.put("walletId", walletId);
            requestBody.put("stockTicker", stockTicker);
            requestBody.put("quantity", quantity);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/transactions/buy"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean success = (response.statusCode() >= 200 && response.statusCode() < 300);
            System.out.println("      Result: " + (success ? "SUCCESS" : "FAILED") +
                               " (Status: " + response.statusCode() + ")");

            if (!success) {
                System.out.println("      Error details: " + response.body());
            }

            return success;
        } catch (IOException | InterruptedException e) {
            System.err.println("      Error buying stock: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    /**
     * Sells stock via API call
     */
    private static boolean sellStock(String walletId, String stockTicker, BigDecimal quantity) {
        try {
            System.out.println("    Selling stock: " + stockTicker + ", Quantity: " + quantity);

            JSONObject requestBody = new JSONObject();
            requestBody.put("walletId", walletId);
            requestBody.put("stockTicker", stockTicker);
            requestBody.put("quantity", quantity);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/transactions/sell"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean success = (response.statusCode() >= 200 && response.statusCode() < 300);
            System.out.println("      Result: " + (success ? "SUCCESS" : "FAILED") +
                               " (Status: " + response.statusCode() + ")");

            if (!success) {
                System.out.println("      Error details: " + response.body());
                // Note: Selling may fail if not enough stocks owned, which is expected in this simulation
            }

            return success;
        } catch (IOException | InterruptedException e) {
            System.err.println("      Error selling stock: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    /**
     * Helper method to get a random stock ticker from the list
     */
    private static String getRandomStockTicker(List<String> tickers) {
        if (tickers.isEmpty()) {
            throw new IllegalArgumentException("No stock tickers available");
        }
        return tickers.get(random.nextInt(tickers.size()));
    }

    /**
     * Helper method to generate a random amount between min and max
     */
    private static BigDecimal generateRandomAmount(double min, double max) {
        double amount = min + (max - min) * random.nextDouble();
        return new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Simple class to store investor ID and wallet ID pair
     */
    private static class InvestorWallet {
        final String investorId;
        final String walletId;

        InvestorWallet(String investorId, String walletId) {
            this.investorId = investorId;
            this.walletId = walletId;
        }
    }
}
