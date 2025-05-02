package Routes;

import java.io.IOException;
import java.net.InetSocketAddress;

import Routes.Handlers.TransactionsHandler;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.HttpServer;

import Routes.Handlers.InvestorsHandler;
import Routes.Handlers.WalletsHandler;
import Routes.Handlers.StocksHandler;
import Routes.Handlers.ReportsHandler;

import Services.HoldingService;
import Services.InvestorService;
import Services.TransactionService;
import Services.crudStockService;
import Services.StockHistoryService;

public class RestApiServer {
    private final HttpServer server;
    private final InvestorService investorService;
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final TransactionService transactionService;
    private final crudStockService stockService;
    private final HoldingService holdingService;
    private final StockHistoryService stockHistoryService;


    public RestApiServer(MongoClient client, MongoDatabase db, int port) throws IOException {
        mongoClient = client;
        database = db;
        investorService = new InvestorService(database);
        transactionService = new TransactionService(database, mongoClient);
        stockService = new crudStockService(database);
        holdingService = new HoldingService(database);
        stockHistoryService = new StockHistoryService(database);

        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/investors", new InvestorsHandler(investorService));

        server.createContext("/wallets", new WalletsHandler(investorService, holdingService, transactionService)); // Pass needed services

        server.createContext("/stocks", new StocksHandler(stockService, stockHistoryService));

        server.createContext("/transactions", new TransactionsHandler(transactionService)); // Pass needed services

        server.createContext("/reports", new ReportsHandler(transactionService));

        // default executor
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
    }

    public void start() {
        server.start();
        System.out.println("REST API Server started on port " + server.getAddress().getPort());
    }

    public void stop() {
        System.out.println("Stopping REST API Server...");
        server.stop(0);
        mongoClient.close();
        System.out.println("Server stopped.");
    }
}