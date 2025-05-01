package Routes.Handlers;

import Routes.RoutesUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import Models.Investor;
import Models.Wallet;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import Services.InvestorService;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InvestorsHandler implements HttpHandler {

    private final InvestorService investorService;
    // Pattern to match /investors/{investorId}
    private static final Pattern INVESTOR_ID_PATTERN = Pattern.compile("^/investors/([a-fA-F0-9]{24})$");
    // Pattern to match /investors/{investorId}/wallets
    private static final Pattern INVESTOR_WALLETS_PATTERN = Pattern.compile("^/investors/([a-fA-F0-9]{24})/wallets$");

    public InvestorsHandler(InvestorService investorService) {
        this.investorService = investorService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            // Match specific investor: /investors/{investorId}
            Matcher idMatcher = INVESTOR_ID_PATTERN.matcher(path);
            if (idMatcher.matches()) {
                String investorId = idMatcher.group(1);
                if ("GET".equalsIgnoreCase(method)) {
                    getInvestorById(exchange, investorId); // Implement this method
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return; // Handled
            }

            // Match investor wallets: /investors/{investorId}/wallets
            Matcher walletsMatcher = INVESTOR_WALLETS_PATTERN.matcher(path);
            if (walletsMatcher.matches()) {
                String investorId = walletsMatcher.group(1);
                if ("GET".equalsIgnoreCase(method)) {
                    getInvestorWallets(exchange, investorId); // Move logic from old InvestorWalletsHandler
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return; // Handled
            }

            // Match base route: /investors
            if (path.equals("/investors")) {
                if ("GET".equalsIgnoreCase(method)) {
                    getAllInvestors(exchange); // Move logic from old InvestorsHandler
                } else if ("POST".equalsIgnoreCase(method)) {
                    createInvestor(exchange); // Move logic from old InvestorsHandler
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return; // Handled
            }

            // If none of the patterns matched
            RoutesUtils.sendErrorResponse(exchange, 404, "Not Found: " + path);

        } catch (Exception e) {
            System.err.println("Error processing request " + method + " " + path + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    // --- Private handler methods ---

    private void getAllInvestors(HttpExchange exchange) throws IOException {
        JSONObject responseJson = new JSONObject();
        List<Investor> investors = investorService.getAllInvestors();
        JSONArray arr = new JSONArray();
        for (Investor inv : investors) {
            arr.put(inv.toJson());
        }
        responseJson.put("investors", arr);
        RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
    }

    private void createInvestor(HttpExchange exchange) throws IOException {
        JSONObject responseJson = new JSONObject();
        try {
            String body = RoutesUtils.readRequestBody(exchange);
            JSONObject requestJson = new JSONObject(body);
            Investor investor = new Investor(requestJson); // Assuming constructor handles validation
            Investor createdInvestor = investorService.createInvestor(investor);
            if (createdInvestor != null) {
                RoutesUtils.sendResponse(exchange, 201, createdInvestor.toJson().toString()); // Use toJson
            } else {
                RoutesUtils.sendErrorResponse(exchange, 500, "Failed to create investor");
            }
        } catch (org.json.JSONException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid JSON format: " + e.getMessage());
        } catch (Exception e) {
            RoutesUtils.sendErrorResponse(exchange, 500, "Error creating investor: " + e.getMessage());
        }
    }

    private void getInvestorById(HttpExchange exchange, String investorIdStr) throws IOException {
        JSONObject responseJson = new JSONObject();
        try {
            if (!ObjectId.isValid(investorIdStr)) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Invalid Investor ID format");
                return;
            }
            Investor investor = investorService.getInvestor(investorIdStr); // Assumes getInvestor exists
            if (investor != null) {
                RoutesUtils.sendResponse(exchange, 200, investor.toJson().toString());
            } else {
                RoutesUtils.sendErrorResponse(exchange, 404, "Investor not found with ID: " + investorIdStr);
            }
        } catch (Exception e) {
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }


    private void getInvestorWallets(HttpExchange exchange, String investorIdStr) throws IOException {
        JSONObject responseJson = new JSONObject();
        try {
            if (!ObjectId.isValid(investorIdStr)) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Invalid Investor ID format");
                return;
            }
            Investor investor = investorService.getInvestor(investorIdStr);

            if (investor != null) {
                List<Wallet> wallets = investor.getWallets(); // Assuming getWallets exists
                JSONArray walletsArray = new JSONArray();
                if (wallets != null) {
                    for (Wallet wallet : wallets) {
                        walletsArray.put(wallet.toJson());
                    }
                }
                responseJson.put("wallets", walletsArray);
                RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
            } else {
                RoutesUtils.sendErrorResponse(exchange, 404, "Investor not found with ID: " + investorIdStr);
            }

        } catch (Exception e) {
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}