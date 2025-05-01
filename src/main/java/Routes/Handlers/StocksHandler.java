package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Stock;
import org.json.JSONObject;
import service.crudStockService;

import java.io.IOException;

public class StocksHandler implements HttpHandler {

    private final crudStockService stockService;

    public StocksHandler(crudStockService stockService) {
        this.stockService = stockService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // Route: POST /stocks
        if ("POST".equalsIgnoreCase(method) && path.equals("/stocks")) {
            addStock(exchange);
        }
        // Add future routes like GET /stocks/{ticker} here if needed
        else {
            RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for /stocks path");
        }
    }

    // Handler for POST /stocks
    private void addStock(HttpExchange exchange) throws IOException {
        JSONObject responseJson = new JSONObject();
        try {
            String body = RoutesUtils.readRequestBody(exchange);
            JSONObject requestJson = new JSONObject(body);

            String ticker = requestJson.optString("ticker", null);
            String market = requestJson.optString("market", ""); // Market is optional

            if (ticker == null || ticker.trim().isEmpty()) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Missing required field: ticker");
                return;
            }

            // Call the existing service method which handles API fetch and DB save
            Stock createdStock = stockService.createStock(ticker, market);

            if (createdStock != null) {
                RoutesUtils.sendResponse(exchange, 201, createdStock.toJson().toString()); // Use toJson()
            } else {
                // Check if it already exists or failed to fetch
                Stock existing = stockService.readStock(market.isEmpty() ? ticker : ticker + "." + market);
                if (existing != null) {
                    RoutesUtils.sendErrorResponse(exchange, 409, "Stock " + ticker + (market.isEmpty() ? "" : "." + market) + " already exists.");
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 500, "Failed to create stock. Could not fetch info from external API or save to DB.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error in addStock handler: " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}

