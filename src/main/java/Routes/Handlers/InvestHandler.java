package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import service.InvestmentService;
import model.Transaction;

import java.io.IOException;

public class InvestHandler implements HttpHandler {

    private final InvestmentService investmentService; // Injected service

    // Constructor to inject the InvestmentService
    public InvestHandler(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(method)) {
            this.post(exchange); // Handle POST request
        } else {
            this.methodNotAllowed(exchange); // Handle unsupported methods
        }
    }

    // Handle POST request for investment
    private void post(HttpExchange exchange) throws IOException {
        String body = RoutesUtils.readRequestBody(exchange);
        JSONObject requestJson = new JSONObject(body);

        String investorId = requestJson.optString("investorId");
        String stockTicker = requestJson.optString("stockTicker");
        int quantity = requestJson.optInt("quantity", 0);

        JSONObject responseJson = new JSONObject();
        try {
            // Using the injected InvestmentService to process the investment
            Transaction transaction = investmentService.investInAsset(investorId, stockTicker, quantity);
            responseJson.put("transactionId", transaction.getTransactionId());
            responseJson.put("stockTicker", transaction.getStockId());
            responseJson.put("quantity", transaction.getQuantity());
            responseJson.put("priceAtTransaction", transaction.getPriceAtTransaction());

            RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
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
