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
import Utils.DatabaseSeeder;
import Utils.DatabaseSetupManager;
import Utils.InteractionSimulator;
import Utils.StockSeeder;

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
        }
        Config.AppConfig.setEnabled(enableCache);
        System.out.println("Mise en cache Redis : " + Config.AppConfig.isEnabled());

        MongoClient mongoClient = null;
        RestApiServer apiServer = null;
        MongoDatabase database;

        // Read MongoDB config from environment variables
        String mongoUriEnv = System.getenv("MONGO_URI");
        String dbNameEnv = System.getenv("MONGO_DB_NAME");
        // Provide defaults if environment variables are not set (useful for local dev without docker)
        String mongoConnectionString = (mongoUriEnv != null && !mongoUriEnv.isEmpty()) ? mongoUriEnv : "mongodb://localhost:27017"; // Default to localhost if no env var
        String dbName = (dbNameEnv != null && !dbNameEnv.isEmpty()) ? dbNameEnv : "gestionBourse";
        System.out.println("Connecting to MongoDB at: " + mongoConnectionString);
        System.out.println("Using Database: " + dbName);

        try {
            // Connect to MongoDB
            mongoClient = MongoClients.create(mongoConnectionString);
            database = mongoClient.getDatabase(dbName);
            database.runCommand(new org.bson.Document("ping", 1));
            System.out.println("Successfully connected to MongoDB.");

            boolean runInvestorsSeeding = true;
            boolean runStocksSeedingAndSimulation = true;

            // RUN DATABASE SETUP (indexing and validators setup)
            try {
                System.out.println("\n--- Starting Database Setup ---");
                DatabaseSetupManager.runSetup(database);
                System.out.println("--- Database Setup Finished ---");
            } catch (Exception e) {
                System.err.println("FATAL: Database setup failed. Exiting.");
                e.printStackTrace();
                if (mongoClient != null) mongoClient.close();
                System.exit(1);
            }

            // RUN DATABASE SEEDING
            if (runInvestorsSeeding) {
                try {
                    System.out.println("\n--- Starting Database Seeding ---");
                    DatabaseSeeder.seedInvestors(database);
                    System.out.println("--- Database Seeding Finished ---");
                } catch (Exception e) {
                    System.err.println("WARNING: Database seeding encountered errors. Continuing...");
                    e.printStackTrace();
                }
            } else {
                System.out.println("\n--- Skipping Database Seeding (as configured) ---");
            }

            // INITIALIZE DAOS/SERVICES (AFTER SETUP/SEEDING) === ***
            System.out.println("\nInitializing DAOs and Services...");
            StockDAO stockDao = new StockDAO(database);
            StockPriceHistoryDAO historyDao = new StockPriceHistoryDAO(database);
            crudStockService stockService = new crudStockService(database);
            // Initialize InvestorService etc. if needed by other parts
            System.out.println("DAOs and Services Initialized.");


            // START REST API SERVER (IN A BACKGROUND THREAD)
            System.out.println("\nStarting REST API Server...");
            try {
                // Pass the *already connected* client and database
                apiServer = new RestApiServer(mongoClient, database, 8000);
                Thread apiThread = new Thread(apiServer::start, "RestApiServerThread");
                apiThread.start();
                System.out.println("REST API Server thread started. Waiting briefly...");
                Thread.sleep(1500); // Small delay for server to bind port
            } catch (IOException e) {
                System.err.println("FATAL: Failed to create or start REST API Server.");
                throw e; // Re-throw to main catch block
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("FATAL: Startup interrupted while waiting for API server.");
                throw e; // Re-throw
            }

            // RUN INTERACTION SIMULATION (USING THE RESTAPI - IN A BACKGROUND THREAD)
            if (runStocksSeedingAndSimulation) {

                try {
                    System.out.println("\n--- Starting Stock Database Seeding ---");
                    StockSeeder.seedStocksAndHistory(database);
                    System.out.println("--- Stock Database Seeding Finished ---");
                } catch (Exception e) {
                    System.err.println("WARNING: Stock database seeding encountered errors. Continuing...");
                    e.printStackTrace();
                }


                System.out.println("\n--- Starting Interaction Simulation (Background) ---");
                MongoDatabase finalDatabase = database;
                Thread simulationThread = new Thread(() -> {
                    System.out.println("Simulation Thread: Running...");
                    try {
                        // wait a bit to ensure the API server is ready
                         Thread.sleep(3000);
                        InteractionSimulator.runSimulation(finalDatabase);
                        System.out.println("Simulation Thread: Finished.");
                    } catch (Exception e) {
                        System.err.println("ERROR in background Interaction Simulation thread:");
                        e.printStackTrace();
                    }
                }, "InteractionSimulatorThread");
                simulationThread.start(); // Start the simulation in parallel
            } else {
                System.out.println("\n--- Skipping Interaction Simulation (as configured) ---");
            }

            // START OTHER BACKGROUND TASKS (LIKE CACHE REFRESH)
            System.out.println("\nConfiguring background tasks...");
           // if (Config.AppConfig.isEnabled()) {
                startDailyHistoryCacheRefresh(stockService, historyDao);
           // } else {
            //    System.out.println("Daily history cache refresh task DISABLED.");
          //  }

            // COMPLETION
            System.out.println("\n========================================");
            System.out.println(" Application startup complete. Running...");
            System.out.println("========================================");
            // Main thread continues (and likely just waits for shutdown)

        } catch (Exception e) {
            // Catch any critical startup error that wasn't handled before
            System.err.println("FATAL: Unrecoverable error during application startup: " + e.getMessage());
            e.printStackTrace();
            // Cleanup resources on failure
            if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdownNow();
            if (apiServer != null) apiServer.stop();
            if (mongoClient != null) mongoClient.close();
            System.exit(1);
        }

        // Graceful shutdown hook
        final MongoClient finalMongoClientForHook = mongoClient;
        final RestApiServer finalApiServerForHook = apiServer;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown hook initiated...");
            if (finalApiServerForHook != null) {
                System.out.println("Stopping REST API server...");
                finalApiServerForHook.stop(); // handles client closing
                System.out.println("REST API server stopped.");
            } else if (finalMongoClientForHook != null) {
                // Fallback closing if API server didn't exist or failed to stop cleanly
                System.out.println("Closing MongoDB client (fallback)...");
                try {
                    finalMongoClientForHook.close();
                    System.out.println("MongoDB client closed.");
                } catch (Exception e) {
                    System.err.println("Error closing MongoDB client during shutdown: " + e.getMessage());
                }
            }
            System.out.println("Shutdown complete.");
        }, "AppShutdownHook"));

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

    //    private static void testStockCrud(MongoDatabase db) {
//        crudStockService stockService = null;
//        try {
//            MongoDatabase database = db;
//            // Initialize the CRUD service
//            stockService = new crudStockService(database);
//
//            // Test stock creation for different stocks
//            System.out.println("Testing stock creation...");
//
//            // Create a US stock (Apple)
//            Stock appleStock = stockService.createStock("AAPL", "");
//            if (appleStock != null) {
//                System.out.println("Successfully created Apple stock: " + appleStock);
//            } else {
//                System.out.println("Failed to create Apple stock or it already exists");
//            }
//
//            // Create a European stock (LVMH on Paris exchange)
//            Stock lvmhStock = stockService.createStock("MC", "PA");
//            if (lvmhStock != null) {
//                System.out.println("Successfully created LVMH stock: " + lvmhStock);
//            } else {
//                System.out.println("Failed to create LVMH stock or it already exists");
//            }
//
//            // Read back the created stocks to verify
//            System.out.println("\nTesting stock retrieval...");
//
//            // Read Apple stock
//            Stock retrievedApple = stockService.readStock("AAPL");
//            if (retrievedApple != null) {
//                System.out.println("Retrieved Apple stock: " + retrievedApple);
//            }
//
//            // Read LVMH stock
//            Stock retrievedLVMH = stockService.readStock("MC.PA");
//            if (retrievedLVMH != null) {
//                System.out.println("Retrieved LVMH stock: " + retrievedLVMH);
//            }
//
//            // Test listing all stocks
//            System.out.println("\nListing all stocks in database:");
//            stockService.getAllStocks().forEach(stock -> {
//                System.out.println(" - " + stock);
//            });
//
//            System.out.println("\nStock CRUD operations test completed.");
//        } catch (Exception e) {
//            System.err.println("Error during stock CRUD test: " + e.getMessage());
//            e.printStackTrace();
//
//        }
//    }
//
//    private static void testHistoricalDataFetch(MongoDatabase db) {
//        crudStockService stockService = null;
//        try {
//            MongoDatabase database = db;
//            // Initialize the CRUD service
//            stockService = new crudStockService(database);
//
//            System.out.println("\n---------- Testing Historical Data Fetch ----------");
//
//            // Create a stock with historical data (e.g., Microsoft)
//            Stock msftStock = stockService.createStock("MSFT", "");
//            if (msftStock != null) {
//                System.out.println("Successfully created Microsoft stock with historical data: " + msftStock);
//
//                StockPriceHistoryDAO historyDao = new StockPriceHistoryDAO(database);
//
//                // Get count of historical records saved
//                List<StockPriceHistory> msftHistory = historyDao.findAllByTicker("MSFT");
//                System.out.println("Saved " + msftHistory.size() + " historical data points for Microsoft");
//
//                // Display a few records as samples
//                if (!msftHistory.isEmpty()) {
//                    System.out.println("\nSample historical records:");
//                    int sampleSize = Math.min(5, msftHistory.size());
//                    for (int i = 0; i < sampleSize; i++) {
//                        System.out.println(msftHistory.get(i));
//                    }
//                }
//
//            } else {
//                System.out.println("Failed to create Microsoft stock or it already exists");
//            }
//
//            System.out.println("\nHistorical data fetch test completed.");
//
//        } catch (Exception e) {
//            System.err.println("Error during historical data fetch test: " + e.getMessage());
//            e.printStackTrace();
//
//        }
//    }
}
