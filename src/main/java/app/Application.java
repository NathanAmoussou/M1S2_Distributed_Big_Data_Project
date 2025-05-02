package app;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import CacheDAO.StockHistoryCacheDAO;
import DAO.StockDAO;
import DAO.StockPriceHistoryDAO;
import Models.Stock;
import Models.StockPriceHistory;
import Routes.RestApiServer;
import Services.crudStockService;

public class Application {
    private static ScheduledExecutorService scheduler;

    public static void main(String[] args) {

        // Read cache flag from environment variable or args
        String enableCacheEnv = System.getenv("ENABLE_REDIS_CACHE");
        boolean enableCache = "true".equalsIgnoreCase(enableCacheEnv);

        // Allow overriding with command-line arg (optional)
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--enableRedisCache")) {
                enableCache = true;
                break;
            }
            // Add handling for other flags if needed
        }

        
        Config.AppConfig.setEnabled(enableCache);
        System.out.println("Mise en cache Redis : " + Config.AppConfig.isEnabled());



        // MongoDB connection string
//      // Read MongoDB config from environment variables
        String mongoUriEnv = System.getenv("MONGO_URI");
        String dbNameEnv = System.getenv("MONGO_DB_NAME");

        // Provide defaults if environment variables are not set (useful for local dev without docker)
        String mongoConnectionString = (mongoUriEnv != null && !mongoUriEnv.isEmpty()) ? mongoUriEnv : "mongodb://localhost:27017"; // Default to localhost if no env var
        String dbName = (dbNameEnv != null && !dbNameEnv.isEmpty()) ? dbNameEnv : "gestionBourse";

        System.out.println("Connecting to MongoDB at: " + mongoConnectionString);
        System.out.println("Using Database: " + dbName);

        // Initialiser la connexion MongoDB
        MongoClient mongoClient = null;
        MongoDatabase database = null;
        try {
             mongoClient = MongoClients.create(mongoConnectionString);
             // Optional: Add a simple check to see if connection is successful
             database = mongoClient.getDatabase(dbName);
             database.runCommand(new org.bson.Document("ping", 1)); // Simple ping
             System.out.println("Successfully connected to MongoDB and pinged database.");
        } catch (Exception e) {
             System.err.println("FATAL: Failed to connect to MongoDB at " + mongoConnectionString);
             e.printStackTrace();
             System.exit(1); // Exit if DB connection fails
        }


        // Initialiser les DAO
        StockDAO stockDao = new StockDAO(database);
        StockPriceHistoryDAO historyDao = new StockPriceHistoryDAO(database);


        // Test regular CRUD operations
//        System.out.println("\n---------- Testing Stock CRUD Operations ----------");
//        testStockCrud(mongoConnectionString, dbName);
        
        // Test historical data fetching
        System.out.println("\n---------- Testing Historical Data Fetch ----------");
        testHistoricalDataFetch(mongoConnectionString, dbName);
        
        // L'application continue de tourner...
        System.out.println("Application démarrée, la récupération périodique des indices boursiers est active.");

        RestApiServer apiServer = null;
        
        try {
            // Pass the existing mongoClient and database instances
            apiServer = new RestApiServer(mongoClient, database, 8000);
            // Start the server in a new thread
            new Thread(apiServer::start).start();
             System.out.println("REST API Server thread started.");

        } catch (IOException e) {
             System.err.println("FATAL: Failed to create or start REST API Server.");
            e.printStackTrace();
             if (mongoClient != null) mongoClient.close();
             System.exit(1);
        }

        if (Config.AppConfig.isEnabled()) {
            crudStockService stockService = new crudStockService(database);
            startDailyHistoryCacheRefresh(stockService, historyDao);
        } else {
            System.out.println("Daily history cache refresh task DISABLED because cache is disabled.");
        }

        System.out.println("Application startup complete. Running...");

        /// Graceful shutdown hook (optional but good practice)
         // Use the final variables from the try block
        final MongoClient finalMongoClient = mongoClient;
        final RestApiServer finalApiServer = apiServer; // Assuming apiServer is accessible here

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook initiated...");
             if (scheduler != null && !scheduler.isShutdown()) {
                 System.out.println("Shutting down scheduled executor...");
                 scheduler.shutdown();
                 try {
                     if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                         scheduler.shutdownNow();
                     }
                 } catch (InterruptedException ex) {
                     scheduler.shutdownNow();
                     Thread.currentThread().interrupt();
                 }
                 System.out.println("Scheduler shut down.");
             }
             if (finalApiServer != null) {
                  System.out.println("Stopping REST API server...");
                  finalApiServer.stop(); // Assuming stop() handles client closing now
                  System.out.println("REST API server stopped.");
             } else if (finalMongoClient != null) {
                 // Fallback if API server didn't handle it
                  System.out.println("Closing MongoDB client (fallback)...");
                 finalMongoClient.close();
                  System.out.println("MongoDB client closed.");
             }
             System.out.println("Shutdown complete.");
        }));
    }
    
    /**
     * Test method to verify if the crudStockService works correctly
     */
    private static void testStockCrud(String mongoConnectionString, String dbName) {
        crudStockService stockService = null;
        try {
            MongoDatabase database = MongoClients.create(mongoConnectionString).getDatabase(dbName);
            // Initialize the CRUD service
            stockService = new crudStockService(database);
            
            // Test stock creation for different stocks
            System.out.println("Testing stock creation...");
            
            // Create a US stock (Apple)
            Stock appleStock = stockService.createStock("AAPL", "");
            if (appleStock != null) {
                System.out.println("Successfully created Apple stock: " + appleStock);
            } else {
                System.out.println("Failed to create Apple stock or it already exists");
            }
            
            // Create a European stock (LVMH on Paris exchange)
            Stock lvmhStock = stockService.createStock("MC", "PA");
            if (lvmhStock != null) {
                System.out.println("Successfully created LVMH stock: " + lvmhStock);
            } else {
                System.out.println("Failed to create LVMH stock or it already exists");
            }
            
            // Read back the created stocks to verify
            System.out.println("\nTesting stock retrieval...");
            
            // Read Apple stock
            Stock retrievedApple = stockService.readStock("AAPL");
            if (retrievedApple != null) {
                System.out.println("Retrieved Apple stock: " + retrievedApple);
            }
            
            // Read LVMH stock
            Stock retrievedLVMH = stockService.readStock("MC.PA");
            if (retrievedLVMH != null) {
                System.out.println("Retrieved LVMH stock: " + retrievedLVMH);
            }
            
            // Test listing all stocks
            System.out.println("\nListing all stocks in database:");
            stockService.getAllStocks().forEach(stock -> {
                System.out.println(" - " + stock);
            });
            
            System.out.println("\nStock CRUD operations test completed.");
        } catch (Exception e) {
            System.err.println("Error during stock CRUD test: " + e.getMessage());
            e.printStackTrace();

        }
    }


    /**
     * Test fetching and storing historical data for a specific stock
     */
    private static void testHistoricalDataFetch(String mongoConnectionString, String dbName) {
        crudStockService stockService = null;
        try {
            MongoDatabase database = MongoClients.create(mongoConnectionString).getDatabase(dbName);
            // Initialize the CRUD service
            stockService = new crudStockService(database);

            System.out.println("\n---------- Testing Historical Data Fetch ----------");

            // Create a stock with historical data (e.g., Microsoft)
            Stock msftStock = stockService.createStock("MSFT", "");
            if (msftStock != null) {
                System.out.println("Successfully created Microsoft stock with historical data: " + msftStock);

                StockPriceHistoryDAO historyDao = new StockPriceHistoryDAO(database);

                // Get count of historical records saved
                List<StockPriceHistory> msftHistory = historyDao.findAllByTicker("MSFT");
                System.out.println("Saved " + msftHistory.size() + " historical data points for Microsoft");

                // Display a few records as samples
                if (!msftHistory.isEmpty()) {
                    System.out.println("\nSample historical records:");
                    int sampleSize = Math.min(5, msftHistory.size());
                    for (int i = 0; i < sampleSize; i++) {
                        System.out.println(msftHistory.get(i));
                    }
                }

            } else {
                System.out.println("Failed to create Microsoft stock or it already exists");
            }

            System.out.println("\nHistorical data fetch test completed.");

        } catch (Exception e) {
            System.err.println("Error during historical data fetch test: " + e.getMessage());
            e.printStackTrace();

        }
    }

    private static void startDailyHistoryCacheRefresh(crudStockService stockService, StockPriceHistoryDAO historyDao) {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        Runnable refreshTask = () -> {
            System.out.println(LocalDateTime.now() + " - Starting daily 30-day history cache refresh task...");
            try {
                List<String> tickers = stockService.getAllStockTickers();
                if (tickers == null || tickers.isEmpty()) {
                    System.out.println("No tickers found to refresh cache.");
                    return;
                }
                System.out.println("Found " + tickers.size() + " tickers to process for cache refresh.");

                //todo TMP
                StockHistoryCacheDAO historyCacheDAO = new StockHistoryCacheDAO();

                int refreshedCount = 0;
                int errorCount = 0;
                for (String ticker : tickers) {
                    try {
                        List<StockPriceHistory> last30Days = historyDao.findLastNDaysByTicker(ticker, 30);
                        if (last30Days != null) { // findLastNDaysByTicker retourne liste vide si non trouvé
                            historyCacheDAO.save(ticker, last30Days);
                            refreshedCount++;
                        } else {
                            System.out.println("No recent history found for ticker: " + ticker + " during refresh.");
                            historyCacheDAO.invalidate(ticker);
                        }
                        Thread.sleep(100); // 50ms pause
                    } catch (Exception e) {
                        System.err.println("Error refreshing cache for ticker " + ticker + ": " + e.getMessage());
                        errorCount++;
                    }
                }
                System.out.println(LocalDateTime.now() + " - Daily history cache refresh task finished. Refreshed: " + refreshedCount + ", Errors: " + errorCount);

            } catch (Throwable t) {
                System.err.println(LocalDateTime.now() + " - CRITICAL ERROR in daily history cache refresh task: " + t.getMessage());
                t.printStackTrace();
            }
        };

        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime nextRun = now.withHour(2).withMinute(0).withSecond(0);
        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }

        Duration initialDelay = Duration.between(now, nextRun);
        long initialDelaySeconds = initialDelay.getSeconds();
        long periodSeconds = TimeUnit.HOURS.toSeconds(1);

        System.out.println("Programming the hourly refresh of the history cache.  Next run at: " + nextRun);
        System.out.println("Initial delay: " + initialDelaySeconds + " seconds. Period: " + periodSeconds + " seconds.");

        scheduler.scheduleAtFixedRate(refreshTask, initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);

        scheduler.schedule(refreshTask, 1, TimeUnit.MINUTES); // Ex: Exécuter dans 1 minute
    }
}
