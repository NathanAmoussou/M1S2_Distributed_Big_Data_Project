package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import service.TransactionService;
import model.Transaction;

import java.io.IOException;
import java.math.BigDecimal;

public class TransactionHandler implements HttpHandler {

    private final TransactionService transactionService; // Injected service

    // Constructor to inject the InvestmentService
    public TransactionHandler(TransactionService investmentService) {
        this.transactionService = investmentService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(method)) {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/buy")) {
                this.invest(exchange);
            } else if (path.endsWith("/sell")) {
                this.sell(exchange);
            } else {
                this.methodNotAllowed(exchange);
            }
        } else {
            this.methodNotAllowed(exchange);
        }
    }

    private void invest(HttpExchange exchange) throws IOException {
        String body = RoutesUtils.readRequestBody(exchange);
        JSONObject requestJson = new JSONObject(body);

        String walletId = requestJson.optString("walletId");
        String stockTicker = requestJson.optString("stockTicker");
        BigDecimal quantity = requestJson.optBigDecimal("quantity", BigDecimal.ZERO);

        JSONObject responseJson = new JSONObject();
        try {
            Transaction transaction = transactionService.buyStock(walletId, stockTicker, quantity);
            RoutesUtils.sendResponse(exchange, 200, transaction.toString());
        } catch (Exception e) {
            responseJson.put("error", e.getMessage());
            RoutesUtils.sendResponse(exchange, 500, responseJson.toString());
        }
    }

    private void sell(HttpExchange exchange) throws IOException {
        String body = RoutesUtils.readRequestBody(exchange);
        JSONObject requestJson = new JSONObject(body);

        String walletId = requestJson.optString("walletId");
        String stockTicker = requestJson.optString("stockTicker");
        BigDecimal quantity = requestJson.optBigDecimal("quantity", BigDecimal.ZERO);

        JSONObject responseJson = new JSONObject();
        try {
            Transaction transaction = transactionService.sellStock(walletId, stockTicker, quantity);
            RoutesUtils.sendResponse(exchange, 200, transaction.toString());
        } catch (Exception e) {
            responseJson.put("error", e.getMessage());
            RoutesUtils.sendResponse(exchange, 500, responseJson.toString());
        }
    }


    // Handle unsupported methods (for example, GET, PUT, DELETE)
    private void methodNotAllowed(HttpExchange exchange) throws IOException {
        JSONObject errorJson = new JSONObject();
        errorJson.put("error", "Méthode non autorisée");
        RoutesUtils.sendResponse(exchange, 405, errorJson.toString());
    }
}
