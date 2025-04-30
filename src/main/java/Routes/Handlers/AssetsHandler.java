package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import service.crudStockService; // Corrected service name

import java.io.IOException;
import java.util.List;

public class AssetsHandler implements HttpHandler {

    private final crudStockService stockService; // Use the stock service

    public AssetsHandler(crudStockService stockService) {
        this.stockService = stockService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            getAllAssets(exchange);
        } else {
            JSONObject responseJson = new JSONObject();
            responseJson.put("error", "Méthode non autorisée");
            RoutesUtils.sendResponse(exchange, 405, responseJson.toString());
        }
    }

    private void getAllAssets(HttpExchange exchange) throws IOException {
        JSONObject responseJson = new JSONObject();
        try {
            // We need a method in crudStockService to get just the tickers
            List<String> tickers = stockService.getAllStockTickers(); // Let's assume we create this method

            if (tickers != null) {
                JSONArray tickersArray = new JSONArray(tickers);
                responseJson.put("assets", tickersArray);
                RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
            } else {
                responseJson.put("error", "Could not retrieve assets");
                RoutesUtils.sendResponse(exchange, 500, responseJson.toString());
            }
        } catch (Exception e) {
            responseJson.put("error", "Internal server error: " + e.getMessage());
            RoutesUtils.sendResponse(exchange, 500, responseJson.toString());
        }
    }
}