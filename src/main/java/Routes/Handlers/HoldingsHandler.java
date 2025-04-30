package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Holding;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import service.TransactionService; // Assuming holdings logic is here or in a dedicated HoldingsService
import service.HoldingService;
import java.io.IOException;
import java.util.List;

public class HoldingsHandler implements HttpHandler {

    private final HoldingService holdingService; // Or a dedicated HoldingsService

    public HoldingsHandler(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // e.g., /holdings/wallet/605c7d5d80e9a43a8e1f8b1a

        if ("GET".equalsIgnoreCase(method) && path.startsWith("/holdings/wallet/")) {
            getHoldingsByWalletId(exchange, path);
        } else {
            JSONObject responseJson = new JSONObject();
            responseJson.put("error", "Méthode ou chemin non autorisé");
            RoutesUtils.sendResponse(exchange, 405, responseJson.toString());
        }
    }

    private void getHoldingsByWalletId(HttpExchange exchange, String path) throws IOException {
        JSONObject responseJson = new JSONObject();
        try {
            // --- Basic Path Parsing ---
            String[] parts = path.split("/");
            if (parts.length != 4 || !ObjectId.isValid(parts[3])) { // Expecting /holdings/wallet/{walletId}
                responseJson.put("error", "Invalid path format or invalid walletId. Expected /holdings/wallet/{walletId}");
                RoutesUtils.sendResponse(exchange, 400, responseJson.toString());
                return;
            }
            String walletIdStr = parts[3];
            // --- End Basic Path Parsing ---


            List<Holding> holdings = holdingService.getHoldingsByWalletId(walletIdStr);

            if (holdings != null) {
                JSONArray holdingsArray = new JSONArray();
                for (Holding holding : holdings) {
                    holdingsArray.put(holding.toJson()); // Use existing toJson method
                }
                responseJson.put("holdings", holdingsArray);
                RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
            } else {
                // probably means the wallet exists but has no holdings
                responseJson.put("holdings", new JSONArray()); // Return empty array
                RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
            }

        } catch (IllegalArgumentException e) {
            responseJson.put("error", "Invalid Wallet ID format: " + e.getMessage());
            RoutesUtils.sendResponse(exchange, 400, responseJson.toString());
        } catch (Exception e) {
            responseJson.put("error", "Internal server error: " + e.getMessage());
            e.printStackTrace(); // Log the full error server-side
            RoutesUtils.sendResponse(exchange, 500, responseJson.toString());
        }
    }
}