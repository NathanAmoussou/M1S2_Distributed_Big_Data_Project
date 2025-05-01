package Routes.Handlers;

import Routes.RoutesUtils;
import Services.TransactionService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReportsHandler implements HttpHandler {

    private final TransactionService transactionService;

    private static final Pattern TOP_TRADED_PATTERN = Pattern.compile("^/reports/top-traded-stocks$");

    public ReportsHandler(TransactionService transactionService /*, other services */) {
        this.transactionService = transactionService;

    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        System.out.println("ReportsHandler received: " + method + " " + path);

        try {
            Matcher topTradedMatcher = TOP_TRADED_PATTERN.matcher(path);

            if (topTradedMatcher.matches()) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetTopTradedStocks(exchange);
                } else {
                    RoutesUtils.sendErrorResponse(exchange, 405, "Method Not Allowed for " + path);
                }
                return;
            }

            RoutesUtils.sendErrorResponse(exchange, 404, "Not Found: Invalid report path " + path);

        } catch (IllegalArgumentException e) {
            System.err.println("Validation Error in ReportsHandler: " + e.getMessage());
            RoutesUtils.sendErrorResponse(exchange, 400, "Bad Request: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing report request " + method + " " + path + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void handleGetTopTradedStocks(HttpExchange exchange) throws IOException {
        Map<String, String> params = RoutesUtils.parseQueryParams(exchange.getRequestURI().getQuery());
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        int limit = 10;

        try {
            if (params.containsKey("startDate")) {
                startDate = LocalDateTime.parse(params.get("startDate"));
            }
            if (params.containsKey("endDate")) {
                endDate = LocalDateTime.parse(params.get("endDate"));
            }
            if (params.containsKey("limit")) {
                limit = Integer.parseInt(params.get("limit"));
                if (limit <= 0) limit = 10; // Reset if invalid
            }
        } catch (DateTimeParseException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid date format. Use ISO format like YYYY-MM-DDTHH:MM:SS.");
            return;
        } catch (NumberFormatException e) {
            RoutesUtils.sendErrorResponse(exchange, 400, "Invalid limit format. Must be an integer.");
            return;
        }

        List<Document> results = transactionService.getMostTradedStocks(startDate, endDate, limit);

        // Convert List<Document> to JSONArray
        JSONArray responseArray = new JSONArray();
        for (Document doc : results) {
            // Convert Document to JSONObject for consistent API response
            responseArray.put(new JSONObject(doc.toJson()));
        }

        RoutesUtils.sendResponse(exchange, 200, responseArray.toString());
    }

}