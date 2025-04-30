package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Investor;
import model.Wallet;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import service.InvestorService;

import java.io.IOException;
import java.util.List;

public class InvestorWalletsHandler implements HttpHandler {

    private final InvestorService investorService;

    public InvestorWalletsHandler(InvestorService investorService) {
        this.investorService = investorService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // e.g., /investor/605c7d5d80e9a43a8e1f8b1c/wallets

        if ("GET".equalsIgnoreCase(method) && path.contains("/wallets") && path.startsWith("/investor/")) {
            getInvestorWallets(exchange, path);
        } else {
            JSONObject responseJson = new JSONObject();
            responseJson.put("error", "Méthode ou chemin non autorisé");
            RoutesUtils.sendResponse(exchange, 405, responseJson.toString());
        }
    }

    private void getInvestorWallets(HttpExchange exchange, String path) throws IOException {
        JSONObject responseJson = new JSONObject();
        try {
            // --- Basic Path Parsing ---
            // Expecting /investor/{investorId}/wallets
            String[] parts = path.split("/");
            if (parts.length != 4 || !ObjectId.isValid(parts[2])) {
                responseJson.put("error", "Invalid path format or invalid investorId. Expected /investor/{investorId}/wallets");
                RoutesUtils.sendResponse(exchange, 400, responseJson.toString());
                return;
            }
            String investorIdStr = parts[2];
            // --- End Basic Path Parsing ---

            Investor investor = investorService.getInvestor(investorIdStr); // Use existing service method

            if (investor != null) {
                List<Wallet> wallets = investor.getWallets();
                JSONArray walletsArray = new JSONArray();
                if (wallets != null) {
                    for (Wallet wallet : wallets) {
                        walletsArray.put(wallet.toJson());
                    }
                }
                responseJson.put("wallets", walletsArray);
                RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
            } else {
                responseJson.put("error", "Investor not found with ID: " + investorIdStr);
                RoutesUtils.sendResponse(exchange, 404, responseJson.toString());
            }

        } catch (IllegalArgumentException e) {
            responseJson.put("error", "Invalid Investor ID format: " + e.getMessage());
            RoutesUtils.sendResponse(exchange, 400, responseJson.toString());
        } catch (Exception e) {
            responseJson.put("error", "Internal server error: " + e.getMessage());
            e.printStackTrace(); // Log server-side
            RoutesUtils.sendResponse(exchange, 500, responseJson.toString());
        }
    }
}