package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Wallet;
import org.json.JSONObject;
import service.InvestorService;

import java.io.IOException;
import java.math.BigDecimal;


public class WalletAddFundsHandler implements HttpHandler {

    private final InvestorService investorService; // Injected service

    public WalletAddFundsHandler(InvestorService investorService) {
        this.investorService = investorService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            this.get(exchange);
        } else if ("POST".equalsIgnoreCase(method)) {
            this.post(exchange);
        } else if ("PUT".equalsIgnoreCase(method)) {
            this.put(exchange);
        } else if ("DELETE".equalsIgnoreCase(method)) {
            this.delete(exchange);
        } else {
            JSONObject responseJson = new JSONObject();
            responseJson.put("error", "Méthode non autorisée");
            RoutesUtils.sendResponse(exchange, 405, responseJson.toString());
        }
    }

    private void get(HttpExchange exchange) throws IOException {
        JSONObject responseJson = new JSONObject();
        responseJson.put("message", "GET method not implemented yet");
        RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
    }

    private void post(HttpExchange exchange) throws IOException {
        JSONObject responseJson = new JSONObject();
        String body = RoutesUtils.readRequestBody(exchange);

        try {
            JSONObject requestJson = new JSONObject(body);

            // Extract walletId and amount from the request
            String walletId = requestJson.getString("walletId");
            BigDecimal amount = requestJson.getBigDecimal("amount");

            // Call service to add funds
            Wallet updatedWallet = investorService.addFundsToWallet(walletId, amount);

            RoutesUtils.sendResponse(exchange, 201, updatedWallet.toString());
        } catch (Exception e) {
            responseJson.put("error", "Error while adding funds to wallet: " + e.getMessage());
            RoutesUtils.sendResponse(exchange, 500, responseJson.toString());
        }
    }


    private void put(HttpExchange exchange) throws IOException {
        JSONObject responseJson = new JSONObject();
        // Handle PUT request logic (update investor or other functionality)
        responseJson.put("message", "PUT method not implemented yet");
        RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
    }

    private void delete(HttpExchange exchange) throws IOException {
        JSONObject responseJson = new JSONObject();
        // Handle DELETE request logic (delete investor or other functionality)
        responseJson.put("message", "DELETE method not implemented yet");
        RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
    }
}
