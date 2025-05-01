package Routes;

import java.io.IOException;
import java.net.InetSocketAddress;

import Routes.Handlers.TransactionsHandler;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.HttpServer;

// Import NEW consolidated handlers
import Routes.Handlers.InvestorsHandler; // New
import Routes.Handlers.WalletsHandler;   // New
import Routes.Handlers.StocksHandler;    // New

// Keep service imports
import Services.HoldingService;
import Services.InvestorService;
import Services.TransactionService;
import Services.crudStockService;

public class RestApiServer {
    private final HttpServer server;
    private final InvestorService investorService;
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final TransactionService transactionService;
    private final crudStockService stockService;
    private final HoldingService holdingService;

    public RestApiServer(String mongoUri, String dbName, int port) throws IOException {
        mongoClient = MongoClients.create(mongoUri);
        database = mongoClient.getDatabase(dbName);
        investorService = new InvestorService(database);
        transactionService = new TransactionService(database);
        stockService = new crudStockService(database);
        holdingService = new HoldingService(database); // WalletHandler will need this

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Handles: GET /investors, POST /investors, GET /investors/{id}, GET /investors/{id}/wallets
        server.createContext("/investors", new InvestorsHandler(investorService));

        // Handles: POST /wallets/{id}/funds, GET /wallets/{id}/holdings, GET /wallets/{id}/transactions, GET /wallets/{id}
        server.createContext("/wallets", new WalletsHandler(investorService, holdingService, transactionService)); // Pass needed services

        // Handles: GET /stocks, POST /stocks, GET /stocks/{ticker}, GET /stocks/{ticker}/history
        server.createContext("/stocks", new StocksHandler(stockService));

        // Handles: POST /transactions/buy, POST /transactions/sell
        // Note: We map the base "/transactions" path. The handler itself will check the full path.
        server.createContext("/transactions", new TransactionsHandler(transactionService)); // Pass needed services

        // Set a default executor
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
    }

    public void start() {
        server.start();
        System.out.println("REST API Server started on port " + server.getAddress().getPort());
    }

    // Optional: Method to stop the server gracefully
    public void stop() {
        System.out.println("Stopping REST API Server...");
        server.stop(0); // 0 seconds delay
        mongoClient.close(); // Close MongoDB connection
        System.out.println("Server stopped.");
    }
}