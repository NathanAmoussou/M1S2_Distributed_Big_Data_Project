package src.app;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import src.dao.StockDAO;
import src.dao.StockPriceHistoryDAO;
import src.service.StockMarketService;

public class Application {
    public static void main(String[] args) {

        // Initialiser la connexion MongoDB
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("votreDB");

        // Initialiser les DAO
        StockDAO stockDao = new StockDAO(database);
        StockPriceHistoryDAO historyDao = new StockPriceHistoryDAO(database);

        // Créer le service
        StockMarketService service = new StockMarketService(stockDao, historyDao);

        // Lancer la planification
        service.startScheduledUpdates();

        // L'application continue de tourner...
        System.out.println("Application démarrée, la récupération périodique des indices boursiers est active.");
    }
}
