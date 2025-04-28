//package Routes.Handlers;
//
//import Routes.RoutesUtils;
//import com.sun.net.httpserver.HttpExchange;
//import com.sun.net.httpserver.HttpHandler;
//import dao.HoldingsDAO;
//import model.Holdings;
//import org.bson.types.ObjectId;
//import org.json.JSONArray;
//import org.json.JSONObject;
////import service.HoldingsService;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//
//public class HoldingsHandler implements HttpHandler {
//
//    private final HoldingsService holdingsService; // Injected service
//
//    // Constructor to inject the HoldingsService
//    public HoldingsHandler(HoldingsService holdingsService) {
//        this.holdingsService = holdingsService;
//    }
//
//    @Override
//    public void handle(HttpExchange exchange) throws IOException {
//        String method = exchange.getRequestMethod();
//
//        if ("GET".equalsIgnoreCase(method)) {
//            this.getHoldings(exchange); // Handle GET request for holdings
//        } else {
//            this.methodNotAllowed(exchange); // Handle unsupported methods
//        }
//    }
//
//    // Handle GET request for holdings
//    private void getHoldings(HttpExchange exchange) throws IOException {
//        Map<String, String> params = RoutesUtils.parseQueryParams(exchange.getRequestURI().getQuery());
//        String walletId = params.getOrDefault("walletId", "");
//
//        JSONObject responseJson = new JSONObject();
//
//        if (walletId.isEmpty()) {
//            responseJson.put("error", "Paramètre walletId manquant");
//            RoutesUtils.sendResponse(exchange, 400, responseJson.toString());
//            return;
//        }
//
//        try {
//            List<Holdings> holdings = holdingsService.getHoldingsByWalletId(new ObjectId(walletId));
//
//            JSONArray arr = new JSONArray();
//            for (Holdings h : holdings) {
//                JSONObject obj = new JSONObject();
//                obj.put("holdingsId", h.getHoldingsId());
//                obj.put("stockId", h.getStockId());
//                obj.put("quantity", h.getQuantity());
//                obj.put("averagePurchasePrice", h.getAveragePurchasePrice());
//                arr.put(obj);
//            }
//
//            responseJson.put("holdings", arr);
//            RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
//
//        } catch (Exception e) {
//            responseJson.put("error", "Erreur lors de la récupération des holdings: " + e.getMessage());
//            RoutesUtils.sendResponse(exchange, 500, responseJson.toString());
//        }
//    }
//
//    // Handle unsupported methods (for example, POST, PUT, DELETE)
//    private void methodNotAllowed(HttpExchange exchange) throws IOException {
//        JSONObject errorJson = new JSONObject();
//        errorJson.put("error", "Méthode non autorisée");
//        RoutesUtils.sendResponse(exchange, 405, errorJson.toString());
//    }
//}
