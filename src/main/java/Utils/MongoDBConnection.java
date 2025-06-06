package Utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDBConnection {
    private static MongoDBConnection instance;
    private final MongoClient client;
    private final MongoDatabase database;

    private MongoDBConnection() {
        client = MongoClients.create("mongodb://localhost:27017");
        database = client.getDatabase("gestionBourse");
    }

    public static synchronized MongoDBConnection getInstance() {
        if (instance == null) {
            instance = new MongoDBConnection();
        }
        return instance;
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}