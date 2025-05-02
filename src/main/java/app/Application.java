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

import CacheDAO.StockHistoryCacheDAO;
import Routes.RestApiServer;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import DAO.StockDAO;
import DAO.StockPriceHistoryDAO;
import Models.Stock;
import Models.StockPriceHistory;
import Services.crudStockService;

public class Application {
    private static ScheduledExecutorService scheduler;

    public static void main(String[] args) {

        boolean enableCache = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--enableRedisCache")) {
                enableCache = true;
                break;
            }
        }
        Config.AppConfig.setEnabled(enableCache);
        System.out.println("Mise en cache Redis : " + Config.AppConfig.isEnabled());



        // MongoDB connection string
        String mongoConnectionString = "mongodb://localhost:27017";
        String dbName = "gestionBourse";

        // Initialiser la connexion MongoDB
        MongoClient mongoClient = MongoClients.create(mongoConnectionString);
        MongoDatabase database = mongoClient.getDatabase(dbName);

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

        try {
            RestApiServer apiServer = new RestApiServer(mongoConnectionString, dbName, 8000);
            new Thread(apiServer::start).start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (Config.AppConfig.isEnabled()) {
            crudStockService stockService = new crudStockService(database);
            startDailyHistoryCacheRefresh(stockService, historyDao);
        } else {
            System.out.println("Daily history cache refresh task DISABLED because cache is disabled.");
        }



        // Make sure the main thread doesn't exit
        try {
            // Block the main thread to keep the scheduled task running
            Thread.sleep(Long.MAX_VALUE); // Keeps the application alive
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted: " + e.getMessage());
        } finally {
            // Clean up resources
            mongoClient.close();
        }
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
