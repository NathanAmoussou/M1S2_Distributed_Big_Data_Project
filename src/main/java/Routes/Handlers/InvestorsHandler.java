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
import Models.Address;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InvestorsHandler implements HttpHandler {

    private final InvestorService investorService;
    // Patterns for matching different routes
    // Matches: /investors/{investorId}/addresses/{addressId}
    private static final Pattern INVESTOR_SPECIFIC_ADDRESS_PATTERN = Pattern.compile("^/investors/([a-fA-F0-9]{24})/addresses/([a-fA-F0-9]{24})$");
    // Matches: /investors/{investorId}/addresses
    private static final Pattern INVESTOR_ADDRESSES_COLLECTION_PATTERN = Pattern.compile("^/investors/([a-fA-F0-9]{24})/addresses$");
    // Matches: /investors/{investorId}/wallets
    private static final Pattern INVESTOR_WALLETS_PATTERN = Pattern.compile("^/investors/([a-fA-F0-9]{24})/wallets$");
    // Matches: /investors/{investorId} (Must be checked AFTER more specific patterns)
    private static final Pattern INVESTOR_ID_PATTERN = Pattern.compile("^/investors/([a-fA-F0-9]{24})$");
    // Matches: /investors or /investors/
    private static final Pattern BASE_INVESTORS_PATTERN = Pattern.compile("^/investors/?$");
    // Matches: /investors/lookup
    private static final Pattern INVESTOR_LOOKUP_PATTERN = Pattern.compile("^/investors/lookup$");

    // Matches: /investors/{investorId}/wallets
    private static final Pattern INVESTOR_WALLETS_COLLECTION_PATTERN = Pattern.compile("^/investors/([a-fA-F0-9]{24})/wallets$");


    public InvestorsHandler(InvestorService investorService) {
        this.investorService = investorService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        System.out.println("InvestorsHandler received: " + method + " " + path); // Debug log

        try {
            Matcher specificAddressMatcher = INVESTOR_SPECIFIC_ADDRESS_PATTERN.matcher(path);
            Matcher addressCollectionMatcher = INVESTOR_ADDRESSES_COLLECTION_PATTERN.matcher(path);
            Matcher walletsMatcher = INVESTOR_WALLETS_PATTERN.matcher(path);
            Matcher idMatcher = INVESTOR_ID_PATTERN.matcher(path);
            Matcher baseMatcher = BASE_INVESTORS_PATTERN.matcher(path);
            Matcher lookupMatcher = INVESTOR_LOOKUP_PATTERN.matcher(path);

            if (lookupMatcher.matches()) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleInvestorLookup(exchange);
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return;
            }

            // Route: /investors/{investorId}/addresses/{addressId}
            if (specificAddressMatcher.matches()) {
                String investorId = specificAddressMatcher.group(1);
                String addressId = specificAddressMatcher.group(2);
                switch (method.toUpperCase()) {
                    case "GET":
                        // Optional: Implement getInvestorSingleAddress if needed
                        RoutesUtils.sendErrorResponse(exchange, 501, "GET specific address not implemented yet.");
                        break;
                    case "PUT":
                        handleUpdateAddress(exchange, investorId, addressId);
                        break;
                    case "DELETE":
                        handleRemoveAddress(exchange, investorId, addressId);
                        break;
                    default:
                        RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                        break;
                }
                return; // Handled
            }

            // Route: /investors/{investorId}/addresses
            Matcher walletsCollectionMatcher = INVESTOR_WALLETS_COLLECTION_PATTERN.matcher(path);
            if (walletsCollectionMatcher.matches()) {
                String investorId = walletsCollectionMatcher.group(1);
                if ("POST".equalsIgnoreCase(method)) {
                    handleCreateWallet(exchange, investorId);
                } else if ("GET".equalsIgnoreCase(method)) {
                    // This GET was already implemented to list wallets
                    getInvestorWallets(exchange, investorId);
                }
                else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return; // Handled
            }


            // Route: /investors/{investorId}/addresses
            if (addressCollectionMatcher.matches()) {
                String investorId = addressCollectionMatcher.group(1);
                switch (method.toUpperCase()) {
                    case "GET":
                        handleGetInvestorAddresses(exchange, investorId);
                        break;
                    case "POST":
                        handleAddAddress(exchange, investorId);
                        break;
                    default:
                        RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                        break;
                }
                return; // Handled
            }

            // Route: /investors/{investorId}/wallets
            if (walletsMatcher.matches()) {
                String investorId = walletsMatcher.group(1);
                if ("GET".equalsIgnoreCase(method)) {
                    getInvestorWallets(exchange, investorId);
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return; // Handled
            }

            // Route: /investors/{investorId}
            if (idMatcher.matches()) {
                String investorId = idMatcher.group(1);
                switch (method.toUpperCase()) {
                    case "GET":
                        getInvestorById(exchange, investorId);
                        break;
                    case "PUT":
                        handleUpdateInvestorDetails(exchange, investorId);
                        break;
                    case "DELETE":
                        handleDeleteInvestor(exchange, investorId);
                        break;
                    default:
                        RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                        break;
                }
                return;
            }

            // Route: /investors
            if (baseMatcher.matches()) {
                if ("GET".equalsIgnoreCase(method)) {
                    getAllInvestors(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    createInvestor(exchange);
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return; // Handled
            }

            // If none of the patterns matched
            RoutesUtils.sendErrorResponse(exchange, 404, "Not Found: " + path);

        } catch (IllegalArgumentException e) {
            // Catch validation errors from service/ObjectId
            System.err.println("Validation Error: " + e.getMessage());
            RoutesUtils.sendErrorResponse(exchange, 400, "Bad Request: " + e.getMessage());
        } catch (RuntimeException e) {
            // Catch service layer errors (like Not Found, Cannot Remove Last Address)
            System.err.println("Runtime Error: " + e.getMessage());
            // Determine appropriate status code based on error (404, 409, etc.) - defaulting to 400/500 for now
            // Checking message content could refine this.
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
                RoutesUtils.sendErrorResponse(exchange, 404, "Not Found: " + e.getMessage());
            } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("cannot remove last address")) {
                RoutesUtils.sendErrorResponse(exchange, 400, "Bad Request: " + e.getMessage()); // Or 409 Conflict
            } else {
                e.printStackTrace(); // Log unexpected runtime errors fully
                RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        } catch (Exception e) {
            // Catch broader errors (IO, JSON parsing, etc.)
            System.err.println("General Error processing request " + method + " " + path + ": " + e.getMessage());
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

    private void handleGetInvestorAddresses(HttpExchange exchange, String investorId) throws IOException {
        Investor investor = investorService.getInvestor(investorId); // Handles validation & not found
        if (investor != null) {
            List<Address> addresses = investor.getAddresses();
            JSONArray addressesArray = new JSONArray();
            if (addresses != null) { // Should always have at least one if investor exists
                for (Address address : addresses) {
                    addressesArray.put(address.toJSON()); // Use model's toJson
                }
            }
            JSONObject responseJson = new JSONObject();
            responseJson.put("addresses", addressesArray);
            RoutesUtils.sendResponse(exchange, 200, responseJson.toString());
        } else {
            RoutesUtils.sendErrorResponse(exchange, 404, "Investor not found with ID: " + investorId);
        }
        // Exceptions caught by main handle method
    }

    // Address handling methods
    private void handleAddAddress(HttpExchange exchange, String investorId) throws IOException {
        try {
            String body = RoutesUtils.readRequestBody(exchange);
            JSONObject requestJson = new JSONObject(body);
            Address newAddress = new Address(requestJson);

            Investor updatedInvestor = investorService.addAddressToInvestor(investorId, newAddress);

            Address addedAddress = updatedInvestor.getAddresses().stream()
                    .filter(a -> a.getAddressId().equals(newAddress.getAddressId()))
                    .findFirst()
                    .orElse(null); // Should exist

            if (addedAddress != null) {
                RoutesUtils.sendResponse(exchange, 201, addedAddress.toJSON().toString()); // 201 Created
            } else {
                System.err.println("Error: Added address not found in updated investor object. Investor: " + updatedInvestor.getInvestorId());
                RoutesUtils.sendErrorResponse(exchange, 500, "Failed to confirm address addition.");
            }

        } catch (org.json.JSONException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid JSON format: " + e.getMessage());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error adding address for investor " + investorId + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error while adding address.");
        }
    }

    private void handleUpdateAddress(HttpExchange exchange, String investorId, String addressId) throws IOException {
        try {
            String body = RoutesUtils.readRequestBody(exchange);
            JSONObject requestJson = new JSONObject(body);
            Address updatedAddress = new Address(requestJson);

            updatedAddress.setAddressId(new ObjectId(addressId));
            Investor resultingInvestor = investorService.updateInvestorAddress(investorId, addressId, updatedAddress);

            // Find the updated address object in the result to return it
            Address confirmedUpdate = resultingInvestor.getAddresses().stream()
                    .filter(a -> a.getAddressId().toString().equals(addressId))
                    .findFirst()
                    .orElse(null); // Should be found

            if (confirmedUpdate != null) {
                RoutesUtils.sendResponse(exchange, 200, confirmedUpdate.toJSON().toString()); // 200 OK
            } else {
                System.err.println("Error: Updated address not found in resulting investor object. Investor: " + resultingInvestor.getInvestorId());
                RoutesUtils.sendErrorResponse(exchange, 500, "Failed to confirm address update.");
            }

        } catch (org.json.JSONException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid JSON format: " + e.getMessage());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error updating address " + addressId + " for investor " + investorId + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error while updating address.");
        }
    }

    private void handleRemoveAddress(HttpExchange exchange, String investorId, String addressId) throws IOException {
        try {
            // Service handles finding, checking constraints (last address), removing, saving, caching
            investorService.removeAddressFromInvestor(investorId, addressId);
            RoutesUtils.sendResponse(exchange, 204, ""); // 204 No Content for successful DELETE
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error removing address " + addressId + " for investor " + investorId + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error while removing address.");
        }
    }

    private void handleInvestorLookup(HttpExchange exchange) throws IOException {
        Map<String, String> params = RoutesUtils.parseQueryParams(exchange.getRequestURI().getQuery());
        String email = params.get("email");
        String username = params.get("username");
        Investor investor = null;

        try {
            if (email != null && !email.isEmpty()) {
                investor = investorService.getInvestorByEmail(email);
            } else if (username != null && !username.isEmpty()) {
                investor = investorService.getInvestorByUsername(username);
            } else {
                RoutesUtils.sendErrorResponse(exchange, 400, "Missing query parameter: provide 'email' or 'username'.");
                return;
            }

            if (investor != null) {
                RoutesUtils.sendResponse(exchange, 200, investor.toJson().toString());
            } else {
                RoutesUtils.sendErrorResponse(exchange, 404, "Investor not found.");
            }
        } catch (IllegalArgumentException e) {
            // Re-throw for main handler catch block
            throw e;
        } catch (Exception e) {
            System.err.println("Error during investor lookup: " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error during lookup.");
        }
    }

    private void handleCreateWallet(HttpExchange exchange, String investorId) throws IOException {
        try {
            String body = RoutesUtils.readRequestBody(exchange);
            JSONObject requestJson = new JSONObject(body);

            // Create a temporary Wallet object from JSON to pass data to service
            Wallet walletData = new Wallet(); // Use default constructor
            // Extract relevant fields from JSON - Be careful about required fields
            if (requestJson.has("currencyCode")) {
                walletData.setCurrencyCode(requestJson.getString("currencyCode"));
            }
            if (requestJson.has("walletType")) {
                walletData.setWalletType(requestJson.getString("walletType"));
            }
            // Ignore balance from request, it starts at 0

            Wallet createdWallet = investorService.createWalletForInvestor(investorId, walletData);

            RoutesUtils.sendResponse(exchange, 201, createdWallet.toJson().toString()); // Return the new wallet

        } catch (org.json.JSONException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid JSON format: " + e.getMessage());
        } catch (RuntimeException e) {
            throw e; // Re-throw for main handler
        } catch (Exception e) {
            System.err.println("Error creating wallet for investor " + investorId + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error while creating wallet.");
        }
    }

    private void handleUpdateInvestorDetails(HttpExchange exchange, String investorId) throws IOException {
        try {
            String body = RoutesUtils.readRequestBody(exchange);
            JSONObject requestJson = new JSONObject(body);

            // Convert JSON to Map for the service
            Map<String, Object> fieldsToUpdate = requestJson.toMap();

            Investor updatedInvestor = investorService.updateInvestorDetails(investorId, fieldsToUpdate);

            // updatedInvestor will be null if service throws RuntimeException (e.g., not found)
            // which will be caught by the main handler's catch block.
            RoutesUtils.sendResponse(exchange, 200, updatedInvestor.toJson().toString()); // Return updated investor

        } catch (org.json.JSONException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid JSON format: " + e.getMessage());
        } catch (RuntimeException e) {
            throw e; // Re-throw for main handler
        } catch (Exception e) {
            System.err.println("Error updating details for investor " + investorId + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error while updating investor details.");
        }
    }

    private void handleDeleteInvestor(HttpExchange exchange, String investorId) throws IOException {
        try {
            boolean deleted = investorService.deleteInvestor(investorId);
            // If service throws exception (not found, constraints), it's caught by main handler.
            // If it returns normally, deletion was successful.
            RoutesUtils.sendResponse(exchange, 204, ""); // 204 No Content

        } catch (RuntimeException e) {
            throw e; // Re-throw for main handler
        } catch (Exception e) {
            System.err.println("Error deleting investor " + investorId + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error while deleting investor.");
        }
    }
}