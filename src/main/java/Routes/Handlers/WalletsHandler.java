package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import Models.Holding;
import Models.Transaction;
import Models.Wallet;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import Services.HoldingService;
import Services.InvestorService;
import Services.TransactionService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WalletsHandler implements HttpHandler {

    private final InvestorService investorService;
    private final HoldingService holdingService;
    private final TransactionService transactionService;

    // Regex patterns for wallet routes
    // /wallets/{walletId}/funds
    private static final Pattern FUNDS_PATTERN = Pattern.compile("^/wallets/([a-fA-F0-9]{24})/funds$");
    // /wallets/{walletId}/holdings
    private static final Pattern HOLDINGS_PATTERN = Pattern.compile("^/wallets/([a-fA-F0-9]{24})/holdings$");
    // /wallets/{walletId}/transactions
    private static final Pattern TRANSACTIONS_PATTERN = Pattern.compile("^/wallets/([a-fA-F0-9]{24})/transactions$");
    // /wallets/{walletId} (Optional: for getting basic wallet info)
    private static final Pattern WALLET_ID_PATTERN = Pattern.compile("^/wallets/([a-fA-F0-9]{24})$");


    public WalletsHandler(InvestorService investorService, HoldingService holdingService, TransactionService transactionService) {
        this.investorService = investorService;
        this.holdingService = holdingService;
        this.transactionService = transactionService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        System.out.println("WalletHandler received: " + method + " " + path); // Debug logging

        try {
            Matcher matcher;

            matcher = FUNDS_PATTERN.matcher(path);
            if (matcher.matches()) {
                String walletId = matcher.group(1);
                if ("POST".equalsIgnoreCase(method)) {
                    handleAddFunds(exchange, walletId);
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return;
            }

            matcher = HOLDINGS_PATTERN.matcher(path);
            if (matcher.matches()) {
                String walletId = matcher.group(1);
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetHoldings(exchange, walletId);
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return;
            }

            matcher = TRANSACTIONS_PATTERN.matcher(path);
            if (matcher.matches()) {
                String walletId = matcher.group(1);
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetTransactions(exchange, walletId);
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return;
            }

            // Handle getting basic wallet info
            matcher = WALLET_ID_PATTERN.matcher(path);
            if (matcher.matches()) {
                String walletId = matcher.group(1);
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetWalletDetails(exchange, walletId); // Implement this if needed
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return;
            }


            // If no specific wallet route matched
            RoutesUtils.sendErrorResponse(exchange, 404, "Not Found: Invalid wallet path " + path);

        } catch (Exception e) {
            System.err.println("Error processing wallet request " + method + " " + path + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    // --- Private handler methods ---

    private void handleAddFunds(HttpExchange exchange, String walletIdStr) throws IOException {
        // Logic from old WalletAddFundsHandler POST method
        if (!ObjectId.isValid(walletIdStr)) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid Wallet ID format.");
            return;
        }

        String body = RoutesUtils.readRequestBody(exchange);
        try {
            JSONObject requestJson = new JSONObject(body);
            BigDecimal amount = requestJson.optBigDecimal("amount", null);

            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Invalid or missing 'amount'. Must be a positive number.");
                return;
            }

            // Call service to add funds
            Wallet updatedWallet = investorService.addFundsToWallet(walletIdStr, amount);

            if (updatedWallet != null) {
                // 200 OK is often used for updates, 201 might imply creation but OK here
                RoutesUtils.sendResponse(exchange, 200, updatedWallet.toJson().toString());
            } else {
                // This implies the wallet wasn't found by the service method
                RoutesUtils.sendErrorResponse(exchange, 404, "Wallet not found with ID: " + walletIdStr);
            }
        } catch (org.json.JSONException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid JSON format: " + e.getMessage());
        } catch (IllegalArgumentException e) { // Catch specific errors from service if possible
            RoutesUtils.sendErrorResponse(exchange, 400, "Bad request: " + e.getMessage());
        } catch (RuntimeException e) { // E.g., WalletNotFoundException if thrown by service
            RoutesUtils.sendErrorResponse(exchange, 404, "Resource not found: " + e.getMessage());
        }
        catch (Exception e) { // Catch-all
            System.err.println("Error adding funds to wallet " + walletIdStr + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error while adding funds.");
        }
    }


    private void handleGetHoldings(HttpExchange exchange, String walletIdStr) throws IOException {
//        System.out.println("DEBUG GetHoldings: " + walletIdStr);
        if (!ObjectId.isValid(walletIdStr)) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid Wallet ID format.");
            return;
        }

        try {
            List<Holding> holdings = holdingService.getHoldingsByWalletId(walletIdStr);
            JSONArray holdingsArray = new JSONArray();

            // Check if holdings list is null (could indicate wallet not found) vs empty (wallet found, no holdings)
            if (holdings == null) {
                // we assume wallet not found if service returns null
                RoutesUtils.sendErrorResponse(exchange, 404, "Wallet not found or error retrieving holdings for ID: " + walletIdStr);
                return;
            }

            for (Holding holding : holdings) {
                holdingsArray.put(holding.toJson());
            }

            JSONObject responseJson = new JSONObject();
            responseJson.put("holdings", holdingsArray);
            RoutesUtils.sendResponse(exchange, 200, responseJson.toString());

        } catch (Exception e) {
            System.err.println("Error getting holdings for wallet " + walletIdStr + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error while retrieving holdings.");
        }
    }

    private void handleGetTransactions(HttpExchange exchange, String walletIdStr) throws IOException {
        // Logic from old WalletTransactionsHandler
        if (!ObjectId.isValid(walletIdStr)) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid Wallet ID format.");
            return;
        }

        try {
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

            // Similar check as holdings: null vs empty list
            if (transactions == null) {
                // Decide if this means wallet not found or just an error. Let's assume wallet check happens implicitly.
                RoutesUtils.sendErrorResponse(exchange, 500, "Error retrieving transactions for wallet ID: " + walletIdStr);
                return;
            }

            for (Transaction t : transactions) {
                transactionsArray.put(t.toJson());
            }

            JSONObject responseJson = new JSONObject();
            responseJson.put("transactions", transactionsArray);
            RoutesUtils.sendResponse(exchange, 200, responseJson.toString());

        } catch (Exception e) {
            System.err.println("Error getting transactions for wallet " + walletIdStr + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error while retrieving transactions.");
        }
    }

    private void handleGetWalletDetails(HttpExchange exchange, String walletIdStr) throws IOException {
        // Optional: Get basic details of a single wallet
        if (!ObjectId.isValid(walletIdStr)) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid Wallet ID format.");
            return;
        }
        try {
            Wallet wallet = investorService.getWalletById(walletIdStr); // Assuming this method exists

            if (wallet != null) {
                RoutesUtils.sendResponse(exchange, 200, wallet.toJson().toString());
            } else {
                RoutesUtils.sendErrorResponse(exchange, 404, "Wallet not found with ID: " + walletIdStr);
            }
        } catch (Exception e) {
            System.err.println("Error getting details for wallet " + walletIdStr + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error while retrieving wallet details.");
        }
    }
}