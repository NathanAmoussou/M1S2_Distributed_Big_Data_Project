package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Investor;
import org.json.JSONArray;
import org.json.JSONObject;
import service.InvestorService;

import java.io.IOException;
import java.util.List;

public class InvestorsHandler implements HttpHandler {

    private final InvestorService investorService; // Injected service

    public InvestorsHandler(InvestorService investorService) {
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
        System.out.println("GETting all investors");
        List<Investor> investors = investorService.getAllInvestors(); // Using the injected service
        System.out.println("Investors retrieved: " + investors);

        JSONArray arr = new JSONArray();
        for (Investor inv : investors) {
            arr.put(inv.toJson());
        }
        responseJson.put("investors", arr);
        RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
    }

    private void post(HttpExchange exchange) throws IOException {
        JSONObject responseJson = new JSONObject();
        String body = RoutesUtils.readRequestBody(exchange);
        JSONObject requestJson = new JSONObject(body);
        try {
            // Creating the investor from the JSON
            Investor investor = new Investor(requestJson);
            // Using the injected service to create an investor
            Investor createdInvestor = investorService.createInvestor(investor);
            RoutesUtils.sendResponse(exchange, 201, createdInvestor.toString());
        } catch (Exception e) {
            responseJson.put("error", "Error while creating the investor profile: " + e.getMessage());
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
