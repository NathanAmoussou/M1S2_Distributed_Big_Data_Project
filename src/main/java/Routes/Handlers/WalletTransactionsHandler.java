package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Transaction;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import service.TransactionService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class WalletTransactionsHandler implements HttpHandler {

    private final TransactionService transactionService;

    public WalletTransactionsHandler(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // e.g., /wallet/65f4a1b2c3d4e5f6a7b8c9d0/transactions

        // Route: GET /wallet/{walletId}/transactions
        if ("GET".equalsIgnoreCase(method) && path.matches("/wallet/[^/]+/transactions")) {
            getWalletTransactions(exchange, path);
        } else {
            RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed or Invalid Path");
        }
    }

    private void getWalletTransactions(HttpExchange exchange, String path) throws IOException {
        JSONObject responseJson = new JSONObject();
        try {
            // Extract Investor ID
            String[] parts = path.split("/");
            if (parts.length != 4 || !ObjectId.isValid(parts[2])) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Invalid path format or Investor ID. Expected /investors/{investorId}/transactions");
                return;
            }
            String walletIdStr = parts[2];

            // Extract Query Parameters for dates
            Map<String, String> params = RoutesUtils.parseQueryParams(exchange.getRequestURI().getQuery());
            String startDateStr = params.getOrDefault("startDate", null);
            String endDateStr = params.getOrDefault("endDate", null);

            LocalDate startDate = null;
            LocalDate endDate = null;
            try {
                if (startDateStr != null) startDate = LocalDate.parse(startDateStr);
                if (endDateStr != null) endDate = LocalDate.parse(endDateStr);
            } catch (DateTimeParseException e) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Invalid date format. Use YYYY-MM-DD.");
                return;
            }

            // Call service
            List<Transaction> transactions = transactionService.getTransactionsForWallet(walletIdStr, startDate, endDate);

            JSONArray transactionsArray = new JSONArray();
            if (transactions != null) {
                for (Transaction t : transactions) {
                    transactionsArray.put(t.toJson());
                }
            }

            responseJson.put("transactions", transactionsArray);
            RoutesUtils.sendResponse(exchange, 200, responseJson.toString());

        } catch (IllegalArgumentException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid Investor ID format: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in getWalletTransactions handler: " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}