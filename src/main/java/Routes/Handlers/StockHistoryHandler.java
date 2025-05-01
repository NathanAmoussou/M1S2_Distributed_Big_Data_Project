package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.StockPriceHistory;
import org.json.JSONArray;
import org.json.JSONObject;
import service.crudStockService; // Assuming history logic is here for now

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class StockHistoryHandler implements HttpHandler {

    private final crudStockService stockService;

    public StockHistoryHandler(crudStockService stockService) {
        this.stockService = stockService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // e.g., /stocks/AAPL/history

        // Route: GET /stocks/{ticker}/history
        if ("GET".equalsIgnoreCase(method) && path.matches("/stocks/[^/]+/history")) {
            getStockHistory(exchange, path);
        } else {
            RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed or Invalid Path");
        }
    }

    private void getStockHistory(HttpExchange exchange, String path) throws IOException {
        JSONObject responseJson = new JSONObject();
        try {
            // Extract Ticker
            String[] parts = path.split("/");
            if (parts.length != 4) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Invalid path format. Expected /stocks/{ticker}/history");
                return;
            }
            String ticker = parts[2]; // Assuming ticker might include market like AAPL or MC.PA

            // Extract Query Parameters
            Map<String, String> params = RoutesUtils.parseQueryParams(exchange.getRequestURI().getQuery());
            String startDateStr = params.getOrDefault("startDate", null);
            String endDateStr = params.getOrDefault("endDate", null);
            int page = Integer.parseInt(params.getOrDefault("page", "1"));
            int pageSize = Integer.parseInt(params.getOrDefault("pageSize", "100")); // Default 100 records per page

            LocalDate startDate = null;
            LocalDate endDate = null;

            try {
                if (startDateStr != null) startDate = LocalDate.parse(startDateStr);
                if (endDateStr != null) endDate = LocalDate.parse(endDateStr);
            } catch (DateTimeParseException e) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Invalid date format. Use YYYY-MM-DD.");
                return;
            }

            // Basic validation
            if (page < 1) page = 1;
            if (pageSize < 1) pageSize = 1;
            if (pageSize > 1000) pageSize = 1000; // Max limit

            // Call service method (to be created)
            JSONObject historyResult = stockService.getStockHistory(ticker, startDate, endDate, page, pageSize);

            if(historyResult == null || !historyResult.has("data")) {
                RoutesUtils.sendErrorResponse(exchange, 404, "No history found for the specified criteria.");
                return;
            }

            // Send successful response with data and pagination info
            RoutesUtils.sendResponse(exchange, 200, historyResult.toString());

        } catch (NumberFormatException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid pagination parameters (page, pageSize). Must be integers.");
        } catch (Exception e) {
            System.err.println("Error in getStockHistory handler: " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
