package Utils; // Or your preferred package

import java.io.BufferedReader; // Import your stock service
import java.io.IOException; // Import your Stock model
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import Models.Stock;
import Services.crudStockService;

/**
 * Utility class to seed the MongoDB database with standard stock data
 * (simple tickers or ticker.suffix format) and potentially their initial
 * historical prices using the crudStockService.
 */
public class StockSeeder {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "gestionBourse";
    private static final String TICKER_RESOURCE_FILE = "StockTickers.txt"; // Name of your resource file
    private static final long TICKER_PROCESSING_DELAY_MS = 200; 

    /**
     * Seeds stocks and potentially their history using crudStockService.
     * Reads tickers (simple or ticker.suffix) from the TICKER_RESOURCE_FILE.
     *
     * @param database MongoDatabase instance we use
     */
    public static void seedStocksAndHistory(MongoDatabase database) {
        System.out.println("Starting stock database seeding process...");
        System.out.println(" -> Using Ticker File: " + TICKER_RESOURCE_FILE);
        System.out.println(" -> Connecting to DB: " + DB_NAME);

        // Statistics counters
        int totalTickers = 0;
        int successCount = 0;
        int failureCount = 0;
        long startTime = System.currentTimeMillis();

        crudStockService stockService = null;

        try {
            // Initialize the crudStockService
            try {
                stockService = new crudStockService(database);
                System.out.println(" -> crudStockService initialized.");
            } catch (Exception e) {
                System.err.println("FATAL: Failed to initialize crudStockService: " + e.getMessage());
                e.printStackTrace();
                return; // Cannot proceed without the service
            }

            // Read the tickers from the resource file
            String tickerContent = readResourceFile(TICKER_RESOURCE_FILE);
            if (tickerContent == null || tickerContent.isBlank()) {
                throw new IOException("Failed to read ticker data or file is empty: " + TICKER_RESOURCE_FILE);
            }

            // Split into individual tickers, trim whitespace, remove empty lines
            List<String> tickers = Arrays.stream(tickerContent.split("\\r?\\n")) // Split by newline
                    .map(String::trim)
                    .filter(ticker -> !ticker.isEmpty() && !ticker.startsWith("#")) // Ignore empty lines and comments starting with #
                    .collect(Collectors.toList());

            totalTickers = tickers.size();
            if (totalTickers == 0) {
                 System.out.println("No valid tickers found in " + TICKER_RESOURCE_FILE + ". Exiting.");
                 return;
            }
            System.out.println("Found " + totalTickers + " tickers to process.");

            // Process each ticker
            for (int i = 0; i < totalTickers; i++) {
                String fullTicker = tickers.get(i);
                System.out.printf("Processing ticker: [%s] (%d/%d)...%n", fullTicker, i + 1, totalTickers);

                try {
                    // Parse ticker and suffix (Simplified)
                    TickerParts parts = parseSimpleTickerAndSuffix(fullTicker);

                    Stock processedStock = stockService.createStock(parts.ticker(), parts.suffix());

                    if (processedStock != null) {
                        // Success might mean created now OR already existed and verified
                        System.out.printf(" -> OK: Processed/verified stock: %s%s%n",
                                processedStock.getStockTicker(),
                                (parts.suffix().isEmpty() ? "" : "." + parts.suffix()));
                        successCount++;
                    } else {
                        System.err.println(" -> FAILED: Could not process ticker " + fullTicker + ". Service returned null");
                        failureCount++;
                    }

                } catch (Exception e) {
                    System.err.printf(" -> ERROR processing ticker [%s]: %s%n", fullTicker, e.getMessage());
                    failureCount++;
                }

                if (TICKER_PROCESSING_DELAY_MS > 0 && i < totalTickers - 1) {
                    try {
                        Thread.sleep(TICKER_PROCESSING_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        System.err.println("Seeding interrupted.");
                        break;
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading ticker resource file '" + TICKER_RESOURCE_FILE + "': " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            
            System.err.println("Critical error during stock database seeding process: " + e.getMessage());
            e.printStackTrace();
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000; // Duration in seconds

            System.out.println("\n--- Stock Seeding Summary ---");
            System.out.println("Total tickers attempted: " + totalTickers);
            System.out.println("Successfully processed/verified: " + successCount);
            System.out.println("Failed processing: " + failureCount);
            System.out.println("Duration: " + duration + " seconds");
            System.out.println("-----------------------------");
        }
    }

    /**
     * Helper record to hold ticker and suffix.
     */
    private record TickerParts(String ticker, String suffix) {}

    /**
     * Parses a standard stock ticker string (like "AAPL" or "MC.PA")
     * into base ticker and suffix. Assumes at most one dot, used for the suffix.
     *
     * @param fullTicker The full ticker string.
     * @return TickerParts record containing the base ticker and suffix (empty string if no suffix).
     * Returns empty parts if input is null or blank.
     */
    private static TickerParts parseSimpleTickerAndSuffix(String fullTicker) {
        if (fullTicker == null || fullTicker.isBlank()) {
            return new TickerParts("", "");
        }

        int lastDotIndex = fullTicker.lastIndexOf('.');

        // Case 1: No dot, or dot is the first/last character (invalid for suffix)
        if (lastDotIndex <= 0 || lastDotIndex == fullTicker.length() - 1) {
            return new TickerParts(fullTicker, ""); // Treat entire string as ticker
        }
        // Case 2: Dot is present and in a valid position for a suffix
        else {
            String baseTicker = fullTicker.substring(0, lastDotIndex);
            String suffix = fullTicker.substring(lastDotIndex + 1);
            return new TickerParts(baseTicker, suffix);
        }
    }


    /**
     * Read a file from the classpath resources.
     *
     * @param fileName Name of the file in the resources directory.
     * @return Content of the file as a String.
     * @throws IOException If the file cannot be read or is not found.
     */
    private static String readResourceFile(String fileName) throws IOException {
        // Try getting the resource stream relative to the class loader
        try (InputStream inputStream = StockSeeder.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                // If not found, try relative to the class itself (might help in some envs)
                try (InputStream classStream = StockSeeder.class.getResourceAsStream(fileName)) {
                    if (classStream == null) {
                         throw new IOException("Resource file not found in classpath: " + fileName);
                    }
                     System.out.println("Reading resource file (relative to class): " + fileName);
                     try (BufferedReader reader = new BufferedReader(new InputStreamReader(classStream, StandardCharsets.UTF_8))) {
                         return reader.lines().collect(Collectors.joining("\n"));
                     }
                }
            } else {
                 System.out.println("Reading resource file (relative to classloader): " + fileName);
                 try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                     return reader.lines().collect(Collectors.joining("\n"));
                 }
            }
        }
    }

    /**
     * Main method for standalone execution of the StockSeeder.
     * Connects to MongoDB using configured URI and DB name, then runs the seeding process.
     */
    public static void main(String[] args) {
        System.out.println("--- Standalone Stock Seeder ---");
        System.out.println("Attempting to connect to MongoDB at: " + MONGO_URI);
        MongoClient mongoClient = null;
        try {
            // Connect to MongoDB
            mongoClient = MongoClients.create(MONGO_URI);
            MongoDatabase database = mongoClient.getDatabase(DB_NAME);

            // Verify connection with a simple command (optional but recommended)
            try {
                 database.runCommand(new org.bson.Document("ping", 1));
                 System.out.println("Successfully connected to MongoDB and pinged database '" + DB_NAME + "'.");
            } catch (Exception e) {
                 System.err.println("Warning: Failed to ping MongoDB database '" + DB_NAME + "'. Proceeding anyway... Error: " + e.getMessage());
            }

            System.out.println("\nStarting seeder process...");

            // Run the main seeding logic
            seedStocksAndHistory(database);

            System.out.println("\nSeeding process completed.");

        } catch (Exception e) {
            // Catch MongoDB connection errors or other critical setup issues
            System.err.println("\nCRITICAL ERROR during standalone stock database seeding execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Ensure MongoDB connection is closed
            if (mongoClient != null) {
                System.out.println("\nClosing MongoDB connection...");
                try {
                    mongoClient.close();
                    System.out.println("MongoDB connection closed.");
                } catch (Exception e) {
                    System.err.println("Error closing MongoDB connection: " + e.getMessage());
                }
            }
             System.out.println("--- Seeder Finished ---");
        }
    }
}