package service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.json.JSONObject;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import dao.StockDAO;
import model.Stock;

public class crudStockService {
    private final StockDAO stockDAO;
    private final String mongoConnectionString;
    private final String dbName;
    private MongoClient mongoClient; // Keep a reference to the MongoClient

    public crudStockService(StockDAO stockDAO) {
        this.stockDAO = stockDAO;
        this.mongoConnectionString = null;
        this.dbName = null;
        this.mongoClient = null;
    }

    public crudStockService(String mongoConnectionString, String dbName) {
        this.mongoConnectionString = mongoConnectionString;
        this.dbName = dbName;
        
        // Create MongoClient without try-with-resources to keep it open
        this.mongoClient = MongoClients.create(mongoConnectionString);
        MongoDatabase database = mongoClient.getDatabase(dbName);
        this.stockDAO = new StockDAO(database);
    }

    /**
     * Create a new stock in the database by fetching from the API first
     * @param stockTicker The ticker symbol of the stock to create
     * @param market Optional market identifier (can be empty)
     * @return The created Stock object or null if creation fails
     */
    public Stock createStock(String stockTicker, String market) {

        String dbStockTicker = market != null && !market.isEmpty() ? stockTicker + "." + market : stockTicker;
        try {
            // First check if the stock already exists
            Stock existingStock = stockDAO.findByStockTicker(dbStockTicker);
            if (existingStock != null) {
                System.out.println("Stock with ticker " + dbStockTicker + " already exists");
                return null;
            }

            // Fetch stock info from API
            JSONObject stockInfo = StockApiService.getStockInfo(stockTicker, market);
            if (stockInfo.isEmpty()) {
                System.out.println("Failed to retrieve stock info for " + stockTicker);
                return null;
            }

            // Create new Stock object
            Stock stock = new Stock(
                stockInfo.getString("stock_name"),
                dbStockTicker,
                stockInfo.optString("market", ""), // peut etre ajouter notre market si jamais pas trouvé ? 
                stockInfo.optString("industry", ""),
                stockInfo.optString("sector", ""),
                new BigDecimal(stockInfo.optString("regular_market_price", "0.00")),
                LocalDateTime.now(),
                stockInfo.optString("country", ""),
                stockInfo.optString("currency", "")
            );

            // Save to database
            stockDAO.save(stock);
            System.out.println("Created stock: " + stock);
            return stock;
        } catch (Exception e) {
            System.err.println("Error creating stock: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Read a stock from the database by its ticker
     * @param stockTicker The ticker symbol of the stock to read
     * @return The found Stock object or null if not found
     */
    public Stock readStock(String stockTicker) {
        try {
            Stock stock = stockDAO.findByStockTicker(stockTicker);
            if (stock == null) {
                System.out.println("Stock with ticker " + stockTicker + " not found");
            }
            return stock;
        } catch (Exception e) {
            System.err.println("Error reading stock: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get all stocks from the database
     * @return List of all Stock objects
     */
    public List<Stock> getAllStocks() {
        return stockDAO.findAll();
    }

    /**
     * Update a stock in the database by fetching latest info from API
     * @param stockTicker The ticker symbol of the stock to update
     * @param market Optional market identifier (can be empty)
     * @return The updated Stock object or null if update fails
     */
    public Stock updateStock(String stockTicker, String market) {
        try {
            String dbStockTicker = market != null && !market.isEmpty() ? stockTicker + "." + market : stockTicker;
            
            // First check if the stock exists
            Stock existingStock = stockDAO.findByStockTicker(dbStockTicker);
            if (existingStock == null) {
                System.out.println("Stock with ticker " + dbStockTicker + " not found");
                return null;
            }

            // Fetch latest stock info from API
            JSONObject stockInfo = StockApiService.getStockInfo(stockTicker, market);
            if (stockInfo.isEmpty()) {
                System.out.println("Failed to retrieve stock info for " + stockTicker);
                return null;
            }

            // Update stock with latest information
            existingStock.setStockName(stockInfo.getString("stock_name"));
            existingStock.setMarket(stockInfo.optString("market", market));
            existingStock.setIndustry(stockInfo.optString("industry", ""));
            existingStock.setSector(stockInfo.optString("sector", ""));
            existingStock.setLastPrice(new BigDecimal(stockInfo.optString("regular_market_price", "0.0")));
            existingStock.setCountry(stockInfo.optString("country", ""));
            existingStock.setCurrency(stockInfo.optString("currency", ""));
            
            existingStock.setLastUpdated(LocalDateTime.now());

            // Update in database
            stockDAO.update(existingStock);
            System.out.println("Updated stock: " + existingStock);
            return existingStock;
        } catch (Exception e) {
            System.err.println("Error updating stock: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Delete a stock from the database by its ticker
     * @param stockTicker The ticker symbol of the stock to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteStock(String stockTicker) {
        try {
            Stock existingStock = stockDAO.findByStockTicker(stockTicker);
            if (existingStock == null) {
                System.out.println("Stock with ticker " + stockTicker + " not found");
                return false;
            }

            stockDAO.deleteByStockTicker(stockTicker);
            System.out.println("Deleted stock with ticker: " + stockTicker);
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting stock: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Force update a stock with provided data instead of fetching from API
     * @param stock The Stock object with updated information
     * @return The updated Stock object or null if update fails
     */
    public Stock updateStockManually(Stock stock) {
        try {
            Stock existingStock = stockDAO.findByStockTicker(stock.getStockTicker());
            if (existingStock == null) {
                System.out.println("Stock with ticker " + stock.getStockTicker() + " not found");
                return null;
            }

            stock.setLastUpdated(LocalDateTime.now());
            stockDAO.update(stock);
            System.out.println("Manually updated stock: " + stock);
            return stock;
        } catch (Exception e) {
            System.err.println("Error manually updating stock: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Properly close MongoDB connection when done
     */
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}