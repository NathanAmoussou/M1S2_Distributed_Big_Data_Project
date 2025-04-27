package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.TransactionDAO;
import model.Transaction;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import service.TransactionService;
import util.RedisCacheService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TransactionsHandler implements HttpHandler {

    private final TransactionService transactionService; // Injected service

    // Constructor to inject the TransactionService
    public TransactionsHandler(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            this.getTransactions(exchange); // Handle GET request for transactions
        } else {
            this.methodNotAllowed(exchange); // Handle unsupported methods
        }
    }

    // Handle GET request for transactions
    private void getTransactions(HttpExchange exchange) throws IOException {
        Map<String, String> params = RoutesUtils.parseQueryParams(exchange.getRequestURI().getQuery());
        String walletId = params.getOrDefault("walletId", "");

        JSONObject responseJson = new JSONObject();

        if (walletId.isEmpty()) {
            responseJson.put("error", "Paramètre walletId manquant");
            RoutesUtils.sendResponse(exchange, 400, responseJson.toString());
            return;
        }

        try {
            // Cache check: Check Redis cache first if enabled
            if (AppConfig.isEnabled()) {
                String cachedTransactions = RedisCacheService.getCache("transactions:wallet:" + walletId);
                if (cachedTransactions != null) {
                    responseJson.put("transactions", new JSONArray(cachedTransactions));
                    RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
                    System.out.println("Transactions récupérées depuis le cache.");
                    return;
                }
            }

            // Fetch transactions from the database
            List<Transaction> transactions = transactionService.getTransactionsByWalletId(new ObjectId(walletId));
            JSONArray arr = new JSONArray();
            for (Transaction t : transactions) {
                JSONObject obj = new JSONObject();
                obj.put("transactionId", t.getTransactionId());
                obj.put("stockId", t.getStockId());
                obj.put("quantity", t.getQuantity());
                obj.put("priceAtTransaction", t.getPriceAtTransaction());
                obj.put("transactionType", t.getTransactionTypesId());
                obj.put("transactionStatus", t.getTransactionStatusId());
                obj.put("createdAt", t.getCreatedAt().toString());
                arr.put(obj);
            }

            responseJson.put("transactions", arr);

            // Cache the results if Redis caching is enabled
            if (AppConfig.isEnabled()) {
                RedisCacheService.setCache("transactions:wallet:" + walletId, arr.toString(), AppConfig.CACHE_TTL);
                System.out.println("Transactions mises en cache.");
            }

            RoutesUtils.sendResponse(exchange, 200, responseJson.toString());

        } catch (Exception e) {
            responseJson.put("error", "Erreur lors de la récupération des transactions: " + e.getMessage());
            RoutesUtils.sendResponse(exchange, 500, responseJson.toString());
        }
    }

    // Handle unsupported methods (for example, POST, PUT, DELETE)
    private void methodNotAllowed(HttpExchange exchange) throws IOException {
        JSONObject errorJson = new JSONObject();
        errorJson.put("error", "Méthode non autorisée");
        RoutesUtils.sendResponse(exchange, 405, errorJson.toString());
    }
}
