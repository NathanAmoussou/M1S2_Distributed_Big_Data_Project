package Routes;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import model.Investor;
import model.Transaction;
import model.Wallet;
import org.json.JSONArray;
import org.json.JSONObject;
import service.InvestmentService;
import service.InvestorService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RestApiServer {
    private HttpServer server;
    private InvestorService investorService;
    private InvestmentService investmentService;
    private MongoClient mongoClient;
    private MongoDatabase database;

    public RestApiServer(String mongoUri, String dbName, int port) throws IOException {
        mongoClient = MongoClients.create(mongoUri);
        database = mongoClient.getDatabase(dbName);
        investorService = new InvestorService(database);
        investmentService = new InvestmentService(database);

        server = HttpServer.create(new InetSocketAddress(port), 0);

        //(endpoints)
        server.createContext("/investors", new InvestorsHandler());
        server.createContext("/investors/addFunds", new AddFundsHandler());
        server.createContext("/investors/invest", new InvestHandler());
    }

    public void start() {
        server.start();
        System.out.println("REST API Server démarré sur le port " + server.getAddress().getPort());
    }

    // Méthode utilitaire pour lire le corps de la requête
    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    //investors
    class InvestorsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            JSONObject responseJson = new JSONObject();
            if("GET".equalsIgnoreCase(method)) {
                List<Investor> investors = investorService.getAllInvestors();
                JSONArray arr = new JSONArray();
                for(Investor inv : investors) {
                    JSONObject obj = new JSONObject();
                    obj.put("investorId", inv.getInvestorId());
                    obj.put("username", inv.getUsername());
                    obj.put("name", inv.getName());
                    obj.put("surname", inv.getSurname());
                    obj.put("email", inv.getEmail());
                    arr.put(obj);
                }
                responseJson.put("investors", arr);
                sendResponse(exchange, 200, responseJson.toString());
            } else if("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                JSONObject requestJson = new JSONObject(body);
                Investor investor = new Investor();
                investor.setUsername(requestJson.optString("username"));
                investor.setPassword(requestJson.optString("password"));
                investor.setName(requestJson.optString("name"));
                investor.setSurname(requestJson.optString("surname"));
                investor.setEmail(requestJson.optString("email"));
                investor.setPhoneNumber(requestJson.optString("phoneNumber"));
                investor.setAddressId(requestJson.optString("addressId"));

                try {
                    Investor createdInvestor = investorService.createInvestor(investor);
                    JSONObject obj = new JSONObject();
                    obj.put("investorId", createdInvestor.getInvestorId());
                    obj.put("username", createdInvestor.getUsername());
                    obj.put("name", createdInvestor.getName());
                    obj.put("surname", createdInvestor.getSurname());
                    obj.put("email", createdInvestor.getEmail());
                    responseJson.put("investor", obj);
                    sendResponse(exchange, 201, responseJson.toString());
                } catch(Exception e) {
                    responseJson.put("error", e.getMessage());
                    sendResponse(exchange, 500, responseJson.toString());
                }
            } else {
                responseJson.put("error", "Méthode non autorisée");
                sendResponse(exchange, 405, responseJson.toString());
            }
        }
    }

    //  /investors/addFunds
    // Expects JSON: { "investorId": "id", "amount": 100.0 }
    class AddFundsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if(!"POST".equalsIgnoreCase(exchange.getRequestMethod())){
                JSONObject errorJson = new JSONObject();
                errorJson.put("error", "Méthode non autorisée");
                sendResponse(exchange, 405, errorJson.toString());
                return;
            }
            String body = readRequestBody(exchange);
            JSONObject requestJson = new JSONObject(body);
            String investorId = requestJson.optString("investorId");
            String amountStr = requestJson.optString("amount", "0");
            JSONObject responseJson = new JSONObject();
            try {
                BigDecimal amount = new BigDecimal(amountStr);
                Wallet wallet = investorService.addFunds(investorId, amount);
                responseJson.put("walletId", wallet.getWalletId());
                responseJson.put("newBalance", wallet.getBalance());
                sendResponse(exchange, 200, responseJson.toString());
            } catch(Exception e) {
                responseJson.put("error", e.getMessage());
                sendResponse(exchange, 500, responseJson.toString());
            }
        }
    }

    //  /investors/invest
    // Expects JSON: { "investorId": "id", "stockTicker": "AAPL", "quantity": 10 }
    class InvestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if(!"POST".equalsIgnoreCase(exchange.getRequestMethod())){
                JSONObject errorJson = new JSONObject();
                errorJson.put("error", "Méthode non autorisée");
                sendResponse(exchange, 405, errorJson.toString());
                return;
            }
            String body = readRequestBody(exchange);
            JSONObject requestJson = new JSONObject(body);
            String investorId = requestJson.optString("investorId");
            String stockTicker = requestJson.optString("stockTicker");
            int quantity = requestJson.optInt("quantity", 0);
            JSONObject responseJson = new JSONObject();
            try {
                Transaction transaction = investmentService.investInAsset(investorId, stockTicker, quantity);
                responseJson.put("transactionId", transaction.getTransactionId());
                responseJson.put("stockTicker", transaction.getStockId());
                responseJson.put("quantity", transaction.getQuantity());
                responseJson.put("priceAtTransaction", transaction.getPriceAtTransaction());
                sendResponse(exchange, 200, responseJson.toString());
            } catch(Exception e) {
                responseJson.put("error", e.getMessage());
                sendResponse(exchange, 500, responseJson.toString());
            }
        }
    }

    // Méthode utilitaire pour envoyer la réponse HTTP
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}