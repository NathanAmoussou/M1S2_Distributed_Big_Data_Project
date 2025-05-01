package Routes.Handlers;


import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import Services.TransactionService;
import Models.Transaction; // Assuming this exists and has toJson()

import java.io.IOException;
import java.math.BigDecimal;

public class TransactionsHandler implements HttpHandler {

    private final TransactionService transactionService;

    public TransactionsHandler(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // e.g., /transactions/buy or /transactions/sell

        if ("POST".equalsIgnoreCase(method)) {
            if (path.equals("/transactions/buy")) {
                handleBuy(exchange);
            } else if (path.equals("/transactions/sell")) {
                handleSell(exchange);
            } else {
                RoutesUtils.sendErrorResponse(exchange, 404, "Not Found: " + path);
            }
        } else {
            RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
        }
    }

    private void handleBuy(HttpExchange exchange) throws IOException {
        try {
            String body = RoutesUtils.readRequestBody(exchange);
            JSONObject requestJson = new JSONObject(body);

            // Validate required fields
            String walletId = requestJson.optString("walletId", null);
            String stockTicker = requestJson.optString("stockTicker", null);
            BigDecimal quantity = requestJson.optBigDecimal("quantity", null);

            if (walletId == null || stockTicker == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Missing or invalid required fields: walletId, stockTicker, quantity (must be positive)");
                return;
            }

            // Call service
            Transaction transaction = transactionService.buyStock(walletId, stockTicker, quantity);
            RoutesUtils.sendResponse(exchange, 201, transaction.toJson().toString()); // 201 Created for successful transaction

        } catch (org.json.JSONException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid JSON format: " + e.getMessage());
        } catch (IllegalArgumentException e) { // Catch specific exceptions from service if possible
            RoutesUtils.sendErrorResponse(exchange, 400, "Bad Request: " + e.getMessage());
        } catch (RuntimeException e) { // Catch potential service errors like Insufficient Funds, Stock Not Found etc.
            RoutesUtils.sendErrorResponse(exchange, 409, "Conflict/Error: " + e.getMessage()); // 409 Conflict might be suitable sometimes
        }
        catch (Exception e) { // Generic catch-all
            System.err.println("Error processing BUY transaction: " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void handleSell(HttpExchange exchange) throws IOException {
        try {
            String body = RoutesUtils.readRequestBody(exchange);
            JSONObject requestJson = new JSONObject(body);

            // Validate required fields
            String walletId = requestJson.optString("walletId", null);
            String stockTicker = requestJson.optString("stockTicker", null);
            BigDecimal quantity = requestJson.optBigDecimal("quantity", null);

            if (walletId == null || stockTicker == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Missing or invalid required fields: walletId, stockTicker, quantity (must be positive)");
                return;
            }

            // Call service
            Transaction transaction = transactionService.sellStock(walletId, stockTicker, quantity);
            RoutesUtils.sendResponse(exchange, 201, transaction.toJson().toString()); // 201 Created

        } catch (org.json.JSONException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid JSON format: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Bad Request: " + e.getMessage());
        } catch (RuntimeException e) { // Catch Insufficient Holdings etc.
            RoutesUtils.sendErrorResponse(exchange, 409, "Conflict/Error: " + e.getMessage());
        }
        catch (Exception e) {
            System.err.println("Error processing SELL transaction: " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
}