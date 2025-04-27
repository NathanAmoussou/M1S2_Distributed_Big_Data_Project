package Routes;

import Routes.Handlers.InvestHandler;
import Routes.Handlers.InvestorsHandler;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import config.AppConfig;
import dao.HoldingsDAO;
import dao.StockDAO;
import dao.TransactionDAO;
import model.*;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import service.InvestmentService;
import service.InvestorService;
import util.RedisCacheService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class RestApiServer {
    private final HttpServer server;
    private final InvestorService investorService;
    private final InvestmentService investmentService;
    private final MongoClient mongoClient;
    private final MongoDatabase database;

    public RestApiServer(String mongoUri, String dbName, int port) throws IOException {
        mongoClient = MongoClients.create(mongoUri);
        database = mongoClient.getDatabase(dbName);
        investorService = new InvestorService(database);
        investmentService = new InvestmentService(database);

        server = HttpServer.create(new InetSocketAddress(port), 0);

        //(endpoints)
        server.createContext("/investors", new InvestorsHandler(investorService));
        server.createContext("/investors/addFunds", new AddFundsHandler());

        server.createContext("/investors/invest", new InvestHandler(investmentService));

        server.createContext("/investors/holdings", new HoldingsHandler());
        server.createContext("/investors/transactions", new TransactionsHandler());
        server.createContext("/assets", new AssetsHandler());
        server.createContext("/investors/sell", new SellHandler());
        server.createContext("/assets/transactions", new AssetsTransactionsHandler());
        server.createContext("/investors/wallet", new WalletHandler());
        server.createContext("/investors/update", new UpdateInvestorHandler());
    }

    public void start() {
        server.start();
        System.out.println("REST API Server démarré sur le port " + server.getAddress().getPort());
    }


    //investors

    //  /investors/addFunds
    // Expects JSON: { "investorId": "id", "amount": 100.0 }
//    class AddFundsHandler implements HttpHandler {
//        @Override
//        public void handle(HttpExchange exchange) throws IOException {
//            if(!"POST".equalsIgnoreCase(exchange.getRequestMethod())){
//                JSONObject errorJson = new JSONObject();
//                errorJson.put("error", "Méthode non autorisée");
//                sendResponse(exchange, 405, errorJson.toString());
//                return;
//            }
//            String body = readRequestBody(exchange);
//            JSONObject requestJson = new JSONObject(body);
//            String investorId = requestJson.optString("investorId");
//            String amountStr = requestJson.optString("amount", "0");
//            JSONObject responseJson = new JSONObject();
//            try {
//                BigDecimal amount = new BigDecimal(amountStr);
//                Wallet wallet = investorService.addFunds(investorId, amount);
//                responseJson.put("walletId", wallet.getWalletId());
//                responseJson.put("newBalance", wallet.getBalance());
//                sendResponse(exchange, 200, responseJson.toString());
//            } catch(Exception e) {
//                responseJson.put("error", e.getMessage());
//                sendResponse(exchange, 500, responseJson.toString());
//            }
//        }
//    }

    //  /investors/invest
    // Expects JSON: { "investorId": "id", "stockTicker": "AAPL", "quantity": 10 }


    // Handler pour /investors/holdings (GET)
    // Exemple d’URL: http://localhost:8000/investors/holdings?investorId=123
//    class HoldingsHandler implements HttpHandler {
//        @Override
//        public void handle(HttpExchange exchange) throws IOException {
//            if(!"GET".equalsIgnoreCase(exchange.getRequestMethod())){
//                JSONObject errorJson = new JSONObject();
//                errorJson.put("error", "Méthode non autorisée");
//                sendResponse(exchange, 405, errorJson.toString());
//                return;
//            }
//            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
//            String walletId = params.getOrDefault("walletId", "");
//
//            JSONObject responseJson = new JSONObject();
//            if(walletId.isEmpty()){
//                responseJson.put("error", "Paramètre investorId manquant");
//                sendResponse(exchange, 400, responseJson.toString());
//                return;
//            }
//            HoldingsDAO holdingsDAO = new HoldingsDAO(database.getCollection("holdings"));
//            List<Holdings> holdings = holdingsDAO.findByWalletId(new ObjectId(walletId));
//            JSONArray arr = new JSONArray();
//            for(Holdings h : holdings){
//                JSONObject obj = new JSONObject();
//                obj.put("holdingsId", h.getHoldingsId());
//                obj.put("stockId", h.getStockId());
//                obj.put("quantity", h.getQuantity());
//                obj.put("averagePurchasePrice", h.getAveragePurchasePrice());
//                arr.put(obj);
//            }
//            responseJson.put("holdings", arr);
//            sendResponse(exchange, 200, responseJson.toString());
//        }
//    }

    // Handler pour /investors/transactions (GET)
    // Exemple d’URL: http://localhost:8000/investors/transactions?investorId=123
//    class TransactionsHandler implements HttpHandler {
//        @Override
//        public void handle(HttpExchange exchange) throws IOException {
//            if(!"GET".equalsIgnoreCase(exchange.getRequestMethod())){
//                JSONObject errorJson = new JSONObject();
//                errorJson.put("error", "Méthode non autorisée");
//                sendResponse(exchange, 405, errorJson.toString());
//                return;
//            }
//            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
//            String walletId = params.getOrDefault("walletId", "");
//
//            JSONObject responseJson = new JSONObject();
//            if(walletId.isEmpty()){
//                responseJson.put("error", "Paramètre investorId manquant");
//                sendResponse(exchange, 400, responseJson.toString());
//                return;
//            }
//
//            if(AppConfig.isEnabled()){
//                String cachedTransactions = RedisCacheService.getCache("transactions:wallet:" + walletId);
//                if(cachedTransactions != null) {
//                    responseJson.put("transactions", new JSONArray(cachedTransactions));
//                    sendResponse(exchange, 200, responseJson.toString());
//                    System.out.println("Transactions récupérées depuis le cache.");
//                    return;
//                }
//            }
//
//            TransactionDAO transactionDAO = new TransactionDAO(database.getCollection("transactions"));
//            List<Transaction> transactions = transactionDAO.findByWalletId(new ObjectId(walletId));
//            JSONArray arr = new JSONArray();
//            for(Transaction t : transactions){
//                JSONObject obj = new JSONObject();
//                obj.put("transactionId", t.getTransactionId());
//                obj.put("stockId", t.getStockId());
//                obj.put("quantity", t.getQuantity());
//                obj.put("priceAtTransaction", t.getPriceAtTransaction());
//                obj.put("transactionType", t.getTransactionTypesId());
//                obj.put("transactionStatus", t.getTransactionStatusId());
//                obj.put("createdAt", t.getCreatedAt().toString());
//                arr.put(obj);
//            }
//            responseJson.put("transactions", arr);
//            if(AppConfig.isEnabled()){
//                RedisCacheService.setCache("transactions:wallet:" + walletId, arr.toString(), AppConfig.CACHE_TTL);
//                System.out.println("Transactions mises en cache.");
//            }
//            sendResponse(exchange, 200, responseJson.toString());
//        }
//    }

    // Handler pour /assets (GET) – liste de tous les actifs disponibles
//    class AssetsHandler implements HttpHandler {
//        @Override
//        public void handle(HttpExchange exchange) throws IOException {
//            if(!"GET".equalsIgnoreCase(exchange.getRequestMethod())){
//                JSONObject errorJson = new JSONObject();
//                errorJson.put("error", "Méthode non autorisée");
//                sendResponse(exchange, 405, errorJson.toString());
//                return;
//            }
//            StockDAO stockDAO = new StockDAO(database);
//            List<Stock> stocks = stockDAO.findAll();
//            JSONArray arr = new JSONArray();
//            for(Stock s : stocks) {
//                JSONObject obj = new JSONObject();
//                obj.put("stockName", s.getStockName());
//                obj.put("stockTicker", s.getStockTicker());
//                obj.put("market", s.getMarket());
//                obj.put("industry", s.getIndustry());
//                obj.put("sector", s.getSector());
//                obj.put("lastPrice", s.getLastPrice());
//                arr.put(obj);
//            }
//            JSONObject responseJson = new JSONObject();
//            responseJson.put("assets", arr);
//            sendResponse(exchange, 200, responseJson.toString());
//        }
//    }

//    class SellHandler implements HttpHandler {
//        @Override
//        public void handle(HttpExchange exchange) throws IOException {
//            if(!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
//                JSONObject errorJson = new JSONObject();
//                errorJson.put("error", "Méthode non autorisée");
//                sendResponse(exchange, 405, errorJson.toString());
//                return;
//            }
//            String body = readRequestBody(exchange);
//            JSONObject requestJson = new JSONObject(body);
//            String investorId = requestJson.optString("investorId");
//            String stockTicker = requestJson.optString("stockTicker");
//            int quantity = requestJson.optInt("quantity", 0);
//            JSONObject responseJson = new JSONObject();
//            try {
//                Transaction transaction = investmentService.sellAsset(investorId, stockTicker, quantity);
//                responseJson.put("transactionId", transaction.getTransactionId());
//                responseJson.put("stockTicker", transaction.getStockId());
//                responseJson.put("quantity", transaction.getQuantity());
//                responseJson.put("priceAtTransaction", transaction.getPriceAtTransaction());
//                sendResponse(exchange, 200, responseJson.toString());
//            } catch(Exception e) {
//                responseJson.put("error", e.getMessage());
//                sendResponse(exchange, 500, responseJson.toString());
//            }
//        }
//    }

//    // Handler pour consulter les transactions d'un actif
//    class AssetsTransactionsHandler implements HttpHandler {
//        @Override
//        public void handle(HttpExchange exchange) throws IOException {
//            if(!"GET".equalsIgnoreCase(exchange.getRequestMethod())){
//                JSONObject errorJson = new JSONObject();
//                errorJson.put("error", "Méthode non autorisée");
//                RoutesUtils.sendResponse(exchange, 405, errorJson.toString());
//                return;
//            }
//            Map<String, String> params = RoutesUtils.parseQueryParams(exchange.getRequestURI().getQuery());
//            String stockTicker = params.getOrDefault("stockTicker", "");
//
//            JSONObject responseJson = new JSONObject();
//            if(stockTicker.isEmpty()){
//                responseJson.put("error", "Paramètre stockTicker manquant");
//                sendResponse(exchange, 400, responseJson.toString());
//                return;
//            }
//            TransactionDAO transactionDAO = new TransactionDAO(database.getCollection("transactions"));
//            List<Transaction> transactions = transactionDAO.findByStockId(stockTicker);
//            JSONArray arr = new JSONArray();
//            for(Transaction t : transactions){
//                JSONObject obj = new JSONObject();
//                obj.put("transactionId", t.getTransactionId());
//                obj.put("walletId", t.getWalletId());
//                obj.put("quantity", t.getQuantity());
//                obj.put("priceAtTransaction", t.getPriceAtTransaction());
//                obj.put("transactionType", t.getTransactionTypesId());
//                obj.put("transactionStatus", t.getTransactionStatusId());
//                obj.put("createdAt", t.getCreatedAt().toString());
//                arr.put(obj);
//            }
//            responseJson.put("transactions", arr);
//            sendResponse(exchange, 200, responseJson.toString());
//        }
//    }

//    // Handler pour consulter le portefeuille d'un investisseur
//    class WalletHandler implements HttpHandler {
//        @Override
//        public void handle(HttpExchange exchange) throws IOException {
//            if(!"GET".equalsIgnoreCase(exchange.getRequestMethod())){
//                JSONObject errorJson = new JSONObject();
//                errorJson.put("error", "Méthode non autorisée");
//                sendResponse(exchange, 405, errorJson.toString());
//                return;
//            }
//            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
//            String investorId = params.getOrDefault("investorId", "");
//
//            JSONObject responseJson = new JSONObject();
//            if(investorId.isEmpty()){
//                responseJson.put("error", "Paramètre investorId manquant");
//                sendResponse(exchange, 400, responseJson.toString());
//                return;
//            }
//            Wallet wallet = new dao.WalletDAO(database).findById(investorId);
//            if(wallet == null){
//                responseJson.put("error", "Portefeuille non trouvé");
//                sendResponse(exchange, 404, responseJson.toString());
//                return;
//            }
//            JSONObject walletJson = new JSONObject();
//            walletJson.put("walletId", wallet.getWalletId());
//            walletJson.put("balance", wallet.getBalance());
//            walletJson.put("currencyCode", wallet.getCurrencyCode());
//            responseJson.put("wallet", walletJson);
//            sendResponse(exchange, 200, responseJson.toString());
//        }
//    }

    // Handler pour mettre à jour le profil d'un investisseur
//    class UpdateInvestorHandler implements HttpHandler {
//        @Override
//        public void handle(HttpExchange exchange) throws IOException {
//            if(!"PUT".equalsIgnoreCase(exchange.getRequestMethod())){
//                JSONObject errorJson = new JSONObject();
//                errorJson.put("error", "Méthode non autorisée");
//                sendResponse(exchange, 405, errorJson.toString());
//                return;
//            }
//            String body = readRequestBody(exchange);
//            JSONObject requestJson = new JSONObject(body);
//            String investorId = requestJson.optString("investorId");
//            if(investorId.isEmpty()){
//                JSONObject errorJson = new JSONObject();
//                errorJson.put("error", "Paramètre investorId manquant");
//                sendResponse(exchange, 400, errorJson.toString());
//                return;
//            }
//            Investor investor = investorService.getInvestor(investorId);
//            if(investor == null) {
//                JSONObject errorJson = new JSONObject();
//                errorJson.put("error", "Investisseur non trouvé");
//                sendResponse(exchange, 404, errorJson.toString());
//                return;
//            }
//            // Mise à jour des champs modifiables
//            if(requestJson.has("username")) {
//                investor.setUsername(requestJson.getString("username"));
//            }
//            if(requestJson.has("password")) {
//                investor.setPassword(requestJson.getString("password"));
//            }
//            if(requestJson.has("name")) {
//                investor.setName(requestJson.getString("name"));
//            }
//            if(requestJson.has("surname")) {
//                investor.setSurname(requestJson.getString("surname"));
//            }
//            if(requestJson.has("email")) {
//                investor.setEmail(requestJson.getString("email"));
//            }
//            if(requestJson.has("phoneNumber")) {
//                investor.setPhoneNumber(requestJson.getString("phoneNumber"));
//            }
//            try {
//                Investor updatedInvestor = investorService.updateInvestor(investor);
//                JSONObject responseJson = new JSONObject();
//                responseJson.put("investorId", updatedInvestor.getInvestorId());
//                responseJson.put("username", updatedInvestor.getUsername());
//                responseJson.put("name", updatedInvestor.getName());
//                responseJson.put("surname", updatedInvestor.getSurname());
//                responseJson.put("email", updatedInvestor.getEmail());
//                sendResponse(exchange, 200, responseJson.toString());
//            } catch(Exception e) {
//                JSONObject errorJson = new JSONObject();
//                errorJson.put("error", e.getMessage());
//                sendResponse(exchange, 500, errorJson.toString());
//            }
//        }
//    }
//
}