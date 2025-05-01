package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import Models.Stock;
import Models.StockPriceHistory;
import org.json.JSONArray;
import org.json.JSONObject;
import Services.crudStockService;
import Services.StockHistoryService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


public class StocksHandler implements HttpHandler {

    private final crudStockService stockService;
    private final StockHistoryService stockHistoryService;

    // Regex patterns for stock routes
    // /stocks/{ticker}/history - ticker can contain letters, numbers, dots, dashes
    private static final Pattern HISTORY_PATTERN = Pattern.compile("^/stocks/([a-zA-Z0-9.-]+)/history$");
    // /stocks/{ticker} - same ticker pattern
    private static final Pattern TICKER_PATTERN = Pattern.compile("^/stocks/([a-zA-Z0-9.-]+)$");
    // /stocks (base route)
    private static final Pattern BASE_PATTERN = Pattern.compile("^/stocks/?$"); // Allow optional trailing slash
    // /evolution of a stock
    private static final Pattern EVOLUTION_PATTERN = Pattern.compile("^/stocks/([a-zA-Z0-9.-]+)/evolution$");


    private static final Pattern HISTORY_30D_PATTERN = Pattern.compile("^/stocks/([a-zA-Z0-9.-]+)/history/30d$");

    public StocksHandler(crudStockService stockService, StockHistoryService stockHistoryService) {
        this.stockService = stockService;
        this.stockHistoryService = stockHistoryService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        System.out.println("StockHandler received: " + method + " " + path); // Debug logging


        try {
            Matcher matcher;

            matcher = HISTORY_30D_PATTERN.matcher(path);
            if (matcher.matches()) {
                String ticker = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetStockHistory30d(exchange, ticker); 
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return;
            }

            matcher = EVOLUTION_PATTERN.matcher(path);
            if (matcher.matches()) {
                String ticker = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetStockEvolution(exchange, ticker);
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return;
            }

            // Check for history first (more specific)
            matcher = HISTORY_PATTERN.matcher(path);
            if (matcher.matches()) {
                String ticker = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetStockHistory(exchange, ticker);
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return;
            }

            // Check for specific ticker next
            matcher = TICKER_PATTERN.matcher(path);
            if (matcher.matches()) {
                String ticker = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetStockDetails(exchange, ticker); // Get details for one stock
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return;
            }

            // Check for base /stocks route last
            matcher = BASE_PATTERN.matcher(path);
            if (matcher.matches()) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetStockList(exchange); // Replaces old AssetsHandler functionality
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleAddStock(exchange); // Logic from old StocksHandler
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return;
            }


            // No patterns matched
            RoutesUtils.sendErrorResponse(exchange, 404, "Not Found: Invalid stock path " + path);

        } catch (Exception e) {
            System.err.println("Error processing stock request " + method + " " + path + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    // --- Private handler methods ---

    private void handleGetStockList(HttpExchange exchange) throws IOException {
        // Logic from old AssetsHandler - GET /assets
        JSONObject responseJson = new JSONObject();
        try {
            // Get only tickers (as before)
            List<String> tickers = stockService.getAllStockTickers();

            if (tickers != null) {
                JSONArray tickersArray = new JSONArray(tickers);
                responseJson.put("assets", tickersArray); // Keep 'assets' key for compatibility? Or change to 'tickers'?
                RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
            } else {
                // If service returns null, assume internal error
                RoutesUtils.sendErrorResponse(exchange, 500, "Could not retrieve stock list.");
            }
        } catch (Exception e) {
            System.err.println("Error getting stock list: " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error retrieving stock list.");
        }
    }

    private void handleAddStock(HttpExchange exchange) throws IOException {
        try {
            String body = RoutesUtils.readRequestBody(exchange);
            JSONObject requestJson = new JSONObject(body);

            String ticker = requestJson.optString("ticker", null);
            String market = requestJson.optString("market", ""); // Market is optional

            if (ticker == null || ticker.trim().isEmpty()) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Missing required field: ticker");
                return;
            }

            Stock createdStock = stockService.createStock(ticker, market);

            if (createdStock != null) {
                RoutesUtils.sendResponse(exchange, 201, createdStock.toJson().toString()); // 201 Created
            } else {
                // Check if it failed because it already exists or due to fetch/save error
                String fullTicker = market.isEmpty() ? ticker : ticker + "." + market;
                Stock existing = stockService.readStock(fullTicker); // Check if already in DB
                if (existing != null) {
                    RoutesUtils.sendErrorResponse(exchange, 409, "Conflict: Stock " + fullTicker + " already exists.");
                } else {
                    // If not found and createStock failed, assume fetch/save error
                    RoutesUtils.sendErrorResponse(exchange, 502, "Bad Gateway: Failed to fetch stock info from external source or save to DB for " + fullTicker); // 502 might be appropriate if external API failed
                }
            }
        } catch (org.json.JSONException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid JSON format: " + e.getMessage());
        } catch (Exception e) { // Catch-all for other unexpected errors
            System.err.println("Error adding stock: " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error while adding stock.");
        }
    }

    private void handleGetStockDetails(HttpExchange exchange, String ticker) throws IOException {
        //  Get details for a single stock Ticker might include market (e.g., "MSFT" or "MC.PA")
        try {
            Stock stock = stockService.readStock(ticker); // Assumes readStock takes the full ticker directly

            if (stock != null) {
                RoutesUtils.sendResponse(exchange, 200, stock.toJson().toString());
            } else {
                RoutesUtils.sendErrorResponse(exchange, 404, "Stock not found with ticker: " + ticker);
            }
        } catch (Exception e) {
            System.err.println("Error getting details for stock " + ticker + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error while retrieving stock details.");
        }
    }


    private void handleGetStockHistory(HttpExchange exchange, String ticker) throws IOException {
        try {
            // Extract Query Parameters
            Map<String, String> params = RoutesUtils.parseQueryParams(exchange.getRequestURI().getQuery());
            String startDateStr = params.getOrDefault("startDate", null);
            String endDateStr = params.getOrDefault("endDate", null);
            // Provide defaults for pagination
            int page = 1;
            int pageSize = 100; // Default page size

            try {
                page = Integer.parseInt(params.getOrDefault("page", "1"));
                pageSize = Integer.parseInt(params.getOrDefault("pageSize", "100"));
            } catch (NumberFormatException e) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Invalid pagination parameters (page, pageSize). Must be integers.");
                return;
            }

            // Basic validation for pagination params
            if (page < 1) page = 1;
            if (pageSize < 1) pageSize = 1;
            if (pageSize > 1000) pageSize = 1000; // reasonable max limit

            LocalDate startDate = null;
            LocalDate endDate = null;

            try {
                if (startDateStr != null) startDate = LocalDate.parse(startDateStr);
                if (endDateStr != null) endDate = LocalDate.parse(endDateStr);
            } catch (DateTimeParseException e) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Invalid date format. Use YYYY-MM-DD.");
                return;
            }

            // Call service method (assuming it returns a JSONObject with data and pagination)
            JSONObject historyResult = stockService.getStockHistory(ticker, startDate, endDate, page, pageSize);

            if (historyResult == null) {
                // Assume service returns null on error (e.g., DB issue)
                RoutesUtils.sendErrorResponse(exchange, 500, "Failed to retrieve history data for " + ticker);
                return;
            }

            // Check if data exists within the result (service might return pagination info even with no data for that page)
            if (!historyResult.has("data") || (historyResult.has("data") && historyResult.getJSONArray("data").isEmpty() && page > 1 )) {
                // Distinguish between genuinely no data and being on a page beyond the results
                if (page > 1 && historyResult.has("pagination") && page > historyResult.getJSONObject("pagination").optInt("totalPages", 0)) {
                    RoutesUtils.sendErrorResponse(exchange, 404, "Page number " + page + " out of range for stock history of " + ticker);
                } else {
                    // Could be no data at all, or page 1 has no data
                    RoutesUtils.sendErrorResponse(exchange, 404, "No history data found for the specified criteria for stock: " + ticker);
                }
                return;
            }

            // Send successful response with data and pagination info
            RoutesUtils.sendResponse(exchange, 200, historyResult.toString());

        } catch (Exception e) { // Catch-all for unexpected errors
            System.err.println("Error getting history for stock " + ticker + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error while retrieving stock history.");
        }
    }

    private void handleGetStockEvolution(HttpExchange exchange, String ticker) throws IOException {
        Map<String, String> params = RoutesUtils.parseQueryParams(exchange.getRequestURI().getQuery());
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        try {
            // Require startDate and endDate for this calculation
            if (!params.containsKey("startDate") || !params.containsKey("endDate")) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Missing required query parameters: 'startDate' and 'endDate'.");
                return;
            }
            // Expecting format like YYYY-MM-DDTHH:MM:SS or just YYYY-MM-DD
            // Convert YYYY-MM-DD to start/end of day
            String startDateStr = params.get("startDate");
            String endDateStr = params.get("endDate");

            if (startDateStr.length() == 10) { // YYYY-MM-DD
                startDate = LocalDate.parse(startDateStr).atStartOfDay();
            } else {
                startDate = LocalDateTime.parse(startDateStr);
            }
            if (endDateStr.length() == 10) { // YYYY-MM-DD
                endDate = LocalDate.parse(endDateStr).atTime(LocalTime.MAX); // End of day
            } else {
                endDate = LocalDateTime.parse(endDateStr);
            }

            if (startDate.isAfter(endDate)) {
                RoutesUtils.sendErrorResponse(exchange, 400, "startDate cannot be after endDate.");
                return;
            }

        } catch (DateTimeParseException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid date format. Use ISO format like YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS.");
            return;
        }

        try {
            JSONObject result = stockHistoryService.getStockPercentageChange(ticker, startDate, endDate);
            if (result.has("error")) {
                // Maybe return 404 if data not found? Or keep 200 with error message? Let's use 404
                RoutesUtils.sendErrorResponse(exchange, 404, result.optString("error", "Could not calculate evolution."));
            } else {
                RoutesUtils.sendResponse(exchange, 200, result.toString());
            }
        } catch (Exception e) { // Catch-all for unexpected service errors
            System.err.println("Error getting stock evolution for " + ticker + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error while calculating stock evolution.");
        }
    }

    private void handleGetStockHistory30d(HttpExchange exchange, String ticker) throws IOException {
        try {
            List<StockPriceHistory> historyList = stockHistoryService.getHistoryLast30Days(ticker);

            if (historyList == null || historyList.isEmpty()) {
                RoutesUtils.sendErrorResponse(exchange, 404, "No recent history data (last 30 days) found for stock: " + ticker);
                return;
            }

            JSONArray dataArray = new JSONArray();
            for (StockPriceHistory sph : historyList) {
                dataArray.put(sph.toJson());
            }

            JSONObject responseJson = new JSONObject();
            responseJson.put("ticker", ticker);
            responseJson.put("days", 30);
            responseJson.put("history", dataArray);

            RoutesUtils.sendResponse(exchange, 200, responseJson.toString());

        } catch (Exception e) {
            System.err.println("Error getting 30d history for stock " + ticker + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error while retrieving recent stock history.");
        }
    }

}