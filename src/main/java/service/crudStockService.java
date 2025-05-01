package service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cacheDAO.StockCacheDAO;
import config.AppConfig;
import dao.StockPriceHistoryDAO;
import model.StockPriceHistory;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.client.MongoDatabase;

import dao.StockDAO;
import model.Stock;


public class crudStockService {
    private final StockDAO stockDAO;
    private final MongoDatabase database;
    private final StockCacheDAO stockCacheDAO;


    public crudStockService(MongoDatabase database) {
        this.database = database;
        this.stockDAO = new StockDAO(database);
        this.stockCacheDAO = new StockCacheDAO();

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

                if (AppConfig.isEnabled()){
                    this.stockCacheDAO.save(existingStock,AppConfig.CACHE_TTL);
                }

                return null;
            }

            // Fetch stock info from API
            JSONObject stockInfo = StockPythonYahooApiService.getStockInfo(stockTicker, market);
            if (stockInfo.isEmpty()) {
                System.out.println("Failed to retrieve stock info for " + stockTicker);
                return null;
            }

            // Create new Stock object
            Stock stock = new Stock(
                stockInfo.getString("stock_name"),
                dbStockTicker,
                stockInfo.optString("market", ""),
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

            if (AppConfig.isEnabled()) {
                stockCacheDAO.save(stock,AppConfig.CACHE_TTL);
                stockCacheDAO.invalidateAll();
                stockCacheDAO.invalidateAllTickers();
                System.out.println("Cached stock and invalidated lists.");
            }


            // Now fetch historical data from 2018 to current year in yearly chunks
            fetchHistoricalData(stockTicker, market, dbStockTicker);
            
            return stock;
        } catch (Exception e) {
            System.err.println("Error creating stock: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Fetch historical data for a stock from 2018 to current year
     * @param stockTicker The ticker symbol of the stock
     * @param market Optional market identifier (can be empty)
     * @param dbStockTicker The ticker symbol used in the database
     */
    private void fetchHistoricalData(String stockTicker, String market, String dbStockTicker) {
        try {
            // Get current year
            int currentYear = LocalDateTime.now().getYear();
            StockPriceHistoryDAO historyDAO = new StockPriceHistoryDAO(database);

            System.out.println("Fetching historical data for " + dbStockTicker + " from 2018 to " + currentYear);

            // Fetch data year by year from 2018 to current year
            for (int year = 2018; year <= currentYear; year++) {
                String startDate = year + "-01-01";
                String endDate = year + "-12-31";

                // If current year, only fetch until today
                if (year == currentYear) {
                    LocalDateTime now = LocalDateTime.now();
                    endDate = String.format("%d-%02d-%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
                }

                System.out.println("Fetching data for " + year + " (" + startDate + " to " + endDate + ")");

                // Fetch historical data from API
                JSONArray historicalData = StockPythonYahooApiService.getStockData(stockTicker, market, startDate, endDate);

                if (historicalData.length() > 0) {
                    System.out.println("Retrieved " + historicalData.length() + " historical records for " + year);

                    // Process and save each data point
                    for (int i = 0; i < historicalData.length(); i++) {
                        JSONObject dataPoint = historicalData.getJSONObject(i);

                        // Parse the date from the data point
                        String dateStr = dataPoint.optString("Date", "");
                        LocalDateTime dateTime = null;

                        try {
                            // Handle date format like "Wed, 19 Aug 2020 04:00:00 GMT"
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.ENGLISH);
                            java.util.Date date = sdf.parse(dateStr);
                            dateTime = date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                        } catch (Exception e1) {
                            try {
                                // Try with ISO format
                                dateTime = LocalDateTime.parse(dateStr.replace("Z", ""));
                            } catch (Exception e2) {
                                try {
                                    // Try with date only format (assuming time is midnight)
                                    dateTime = LocalDate.parse(dateStr).atStartOfDay();
                                } catch (Exception e3) {
                                    System.err.println("Could not parse date: " + dateStr);
                                    continue;
                                }
                            }
                        }

                        // Skip if we couldn't parse the date
                        if (dateTime == null) {
                            System.err.println("Skipping data point: " + dataPoint + " due to invalid date format" + dateStr);
                            continue;
                        }

                        // Create StockPriceHistory object
                        StockPriceHistory history = new StockPriceHistory();
                        history.setStockPriceHistoryTicker(dbStockTicker);
                        history.setDateTime(dateTime);

                        // Handle numerical values with safe conversion
                        history.setOpenPrice(safeParseBigDecimal(dataPoint.optString("Open", "0.0")));
                        history.setClosePrice(safeParseBigDecimal(dataPoint.optString("Close", "0.0")));
                        history.setHighPrice(safeParseBigDecimal(dataPoint.optString("High", "0.0")));
                        history.setLowPrice(safeParseBigDecimal(dataPoint.optString("Low", "0.0")));
                        history.setVolume(safeParseBigDecimal(dataPoint.optString("Volume", "0.0")));
                        history.setDividend(safeParseBigDecimal(dataPoint.optString("Dividends", "0.0")));
                        history.setStockSplit(safeParseBigDecimal(dataPoint.optString("Stock Splits", "0.0")));

                        // Save to database
                        historyDAO.save(history);
                    }

                    // Sleep to avoid overloading the API
                    Thread.sleep(1000);
                } else {
                    System.out.println("No historical data available for " + year);
                }
            }

            System.out.println("Finished fetching historical data for " + dbStockTicker);

        } catch (Exception e) {
            System.err.println("Error fetching historical data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Safely parse a String to BigDecimal, handling null or invalid formats
     * @param value The string to parse
     * @return A BigDecimal representation or 0 if parsing fails
     */
    private BigDecimal safeParseBigDecimal(String value) {
        if (value == null || value.isEmpty() || value.equals("null")) {
            return new BigDecimal("0.0");
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return new BigDecimal("0.0");
        }
    }

    /**
     * Read a stock from the database by its ticker
     * @param stockTicker The ticker symbol of the stock to read
     * @return The found Stock object or null if not found
     */
    public Stock readStock(String stockTicker) {
        try {
            if (AppConfig.isEnabled()) {
                Optional<Stock> cachedStock = stockCacheDAO.findByTicker(stockTicker);
                if (cachedStock.isPresent()) {
                    System.out.println("Stock [" + stockTicker + "] found in cache.");
                    return cachedStock.get();
                }
                System.out.println("Cache miss for [" + stockTicker + "] Reading from the DB...");
            }

            Stock stock = stockDAO.findByStockTicker(stockTicker);
            if (stock != null) {
                System.out.println("Stock [" + stockTicker + "] found in DB. Caching...");
                if (AppConfig.isEnabled()) {
                    stockCacheDAO.save(stock, AppConfig.CACHE_TTL);
                }
            } else {
                System.out.println("Stock [" + stockTicker + "] not found in DB.");
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
        try {
            if (AppConfig.isEnabled()) {
                Optional<List<Stock>> cachedStocks = stockCacheDAO.findAll();
                if (cachedStocks.isPresent()) {
                    System.out.println("Inventory list found in cache.");
                    return cachedStocks.get();
                }
                System.out.println("Cache miss for stock list. Read from DB...");
            }

            List<Stock> stocks = stockDAO.findAll();

            // On met en cache la liste des stocks
            if (AppConfig.isEnabled() ){/// && !stocks.isEmpty()) { //TODO est ce qu'il faut quand meme chaché quand la liste est vide??
                System.out.println("Stock list found in DB (" + stocks.size() + " items). Caching...");
                stockCacheDAO.saveAll(stocks, AppConfig.CACHE_TTL);
            }

            return stocks;
        } catch (Exception e) {
            System.err.println("Error getting all stocks: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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
                if (AppConfig.isEnabled()) {
                    stockCacheDAO.delete(dbStockTicker);
                }
                return null;
            }

            // Fetch latest stock info from API
            JSONObject stockInfo = StockPythonYahooApiService.getStockInfo(stockTicker, market);
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

            if (AppConfig.isEnabled()) {
                stockCacheDAO.save(existingStock, AppConfig.CACHE_TTL);
                //pas sûr de mon coup
                stockCacheDAO.invalidateAll();
                stockCacheDAO.invalidateAllTickers();
                System.out.println("Stock [" + dbStockTicker + "] updated in cache and lists invalidated.");
            }

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
                if (AppConfig.isEnabled()) {
                    stockCacheDAO.delete(stockTicker);
                }
                return false;
            }

            stockDAO.deleteByStockTicker(stockTicker);
            System.out.println("Deleted stock with ticker: " + stockTicker);

            if (AppConfig.isEnabled()) {
                stockCacheDAO.delete(stockTicker);
                stockCacheDAO.invalidateAll();
                stockCacheDAO.invalidateAllTickers();
                System.out.println("Stock [" + stockTicker + "] deleted in cache and lists invalidated.");
            }
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
                if (AppConfig.isEnabled()) {
                    stockCacheDAO.delete(stock.getStockTicker());
                }
                return null;
            }

            stock.setLastUpdated(LocalDateTime.now());
            stockDAO.update(stock);
            System.out.println("Manually updated stock: " + stock);

            if (AppConfig.isEnabled()) {
                stockCacheDAO.save(existingStock, AppConfig.CACHE_TTL);
                stockCacheDAO.invalidateAll();
                stockCacheDAO.invalidateAllTickers();
                System.out.println("Stock [" + stock.getStockTicker() + "] manually updated in cache and lists invalidated.");
            }

            return stock;
        } catch (Exception e) {
            System.err.println("Error manually updating stock: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }



// Add this method inside the crudStockService class
    /**
     * Get all stock tickers from the database
     * @return List of all stock ticker strings
     */
    public List<String> getAllStockTickers() {
        try {
            // Try fetching from cache first if enabled
            if (AppConfig.isEnabled()) {
                Optional<List<String>> cachedTickers = stockCacheDAO.findAllTickers();
                if (cachedTickers.isPresent()) {
                    System.out.println("List ticker found in cache.");
                    return cachedTickers.get();
                }
                System.out.println("Cache miss for stock list. Read from DB...");
            }

            List<Stock> stocks = stockDAO.findAll(); // You already have getAllStocks, but this is more direct
            if (stocks == null) {
                return new ArrayList<>(); // Return empty list if null
            }

            List<String> tickers = stocks.stream()
                    .map(Stock::getStockTicker)
                    .collect(Collectors.toList());

            // Cache the result if enabled
            if (AppConfig.isEnabled()) {
                System.out.println("List ticker found in DB: " + tickers.size() + " elements. Caching...");
                stockCacheDAO.saveAllTickers(tickers, AppConfig.CACHE_TTL);
            }

            return tickers;
        } catch (Exception e) {
            System.err.println("Error getting all stock tickers: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(); // Return empty list on error
        }
    }


}