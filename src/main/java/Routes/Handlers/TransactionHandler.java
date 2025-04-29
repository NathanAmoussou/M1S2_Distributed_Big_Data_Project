package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import service.TransactionService;
import model.Transaction;

import java.io.IOException;
import java.math.BigDecimal;

public class InvestHandler implements HttpHandler {

    private final TransactionService investmentService; // Injected service

    // Constructor to inject the InvestmentService
    public InvestHandler(TransactionService investmentService) {
        this.investmentService = investmentService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(method)) {
            this.post(exchange);
        } else {
            this.methodNotAllowed(exchange);
        }
    }

    private void post(HttpExchange exchange) throws IOException {
        String body = RoutesUtils.readRequestBody(exchange);
        JSONObject requestJson = new JSONObject(body);

        String walletId = requestJson.optString("walletId");
        String stockTicker = requestJson.optString("stockTicker");
        BigDecimal quantity = requestJson.optBigDecimal("quantity", BigDecimal.ZERO);

        JSONObject responseJson = new JSONObject();
        try {

            Transaction transaction = investmentService.investInStock(walletId, stockTicker, quantity);

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
