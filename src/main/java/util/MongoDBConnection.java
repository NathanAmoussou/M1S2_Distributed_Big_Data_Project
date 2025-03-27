package util;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDBConnection {
    private static MongoDBConnection instance;
    private MongoClient client;
    private MongoDatabase database;

    private MongoDBConnection() {
        client = MongoClients.create("mongodb://localhost:27017");
        database = client.getDatabase("bado");
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