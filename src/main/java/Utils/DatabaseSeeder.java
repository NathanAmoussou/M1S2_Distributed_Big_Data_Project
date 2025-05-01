package Utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import Models.Investor;
import org.json.JSONArray;
import org.json.JSONObject;
import Services.InvestorService;

/**
 * Utility class to seed the MongoDB database with investor data from a JSON file.
 * Uses the InvestorService to ensure proper business logic is applied during insertion.
 */
public class DatabaseSeeder {

    public static void main(String[] args) {
        System.out.println("Starting database seeding process...");

        // Statistics counters
        int totalInvestors = 0;
        int successCount = 0;
        int failureCount = 0;

        MongoClient mongoClient = null;

        try {
            // Establish MongoDB connection
            String mongoConnectionString = "mongodb://localhost:27017";
            String dbName = "gestionBourse";

            System.out.println(
                "Connecting to MongoDB at " + mongoConnectionString
            );
            mongoClient = MongoClients.create(mongoConnectionString);
            MongoDatabase database = mongoClient.getDatabase(dbName);
            System.out.println(
                "Successfully connected to database: " + dbName
            );

            // Initialize the InvestorService with the database connection
            InvestorService investorService = new InvestorService(database);

            // Read the JSON file from resources
            String jsonContent = readResourceFile(
                "InvestorsDatasCompleted.json"
            );
            if (jsonContent == null || jsonContent.isEmpty()) {
                throw new IOException(
                    "Failed to read investor data or file is empty"
                );
            }

            // Parse the JSON content into an array
            JSONArray investorsArray = new JSONArray(jsonContent);

            totalInvestors = investorsArray.length();
            System.out.println(
                "Found " +
                totalInvestors +
                " investor records in the JSON file."
            );

            // Process each investor
            for (int i = 0; i < investorsArray.length(); i++) {
                JSONObject investorJson = investorsArray.getJSONObject(i);
                String username = investorJson.optString(
                    "username",
                    "Unknown"
                );

                try {
                    System.out.println(
                        "Processing investor: " +
                        username +
                        " (record " +
                        (i + 1) +
                        "/" +
                        totalInvestors +
                        ")"
                    );

                    // Create Investor instance using the constructor that accepts a JSONObject
                    Investor investor = new Investor(investorJson);

                    // Use the InvestorService to insert the investor (applying business logic)
                    Investor createdInvestor = investorService.createInvestor(
                        investor
                    );

                    System.out.println(
                        "Successfully inserted investor: " +
                        createdInvestor.getUsername() +
                        " (ID: " +
                        createdInvestor.getInvestorId() +
                        ")"
                    );
                    successCount++;
                } catch (IllegalArgumentException e) {
                    System.err.println(
                        "Error with investor data format for " +
                        username +
                        ": " +
                        e.getMessage()
                    );
                    failureCount++;
                } catch (Exception e) {
                    System.err.println(
                        "Error processing investor " +
                        username +
                        ": " +
                        e.getMessage()
                    );
                    failureCount++;
                }
            }
        } catch (IOException e) {
            System.err.println(
                "Error reading investor data file: " + e.getMessage()
            );
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println(
                "Critical error during database seeding: " + e.getMessage()
            );
            e.printStackTrace();
        } finally {
            // Close the MongoDB client in the finally block
            if (mongoClient != null) {
                System.out.println("Closing MongoDB connection...");
                mongoClient.close();
            }

            // Print final summary
            System.out.println("\n--- Database Seeding Summary ---");
            System.out.println(
                "Total investor records processed: " + totalInvestors
            );
            System.out.println("Successfully inserted: " + successCount);
            System.out.println("Failed insertions: " + failureCount);
            System.out.println("-----------------------------");
        }
    }

    /**
     * Read a file from the classpath resources
     * @param fileName Name of the file in the resources directory
     * @return Content of the file as a String
     * @throws IOException If the file cannot be read
     */
    private static String readResourceFile(String fileName)
        throws IOException {
        try (
            InputStream inputStream =
                DatabaseSeeder.class.getClassLoader()
                    .getResourceAsStream(fileName)
        ) {
            if (inputStream == null) {
                throw new IOException("Resource file not found: " + fileName);
            }

            try (
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
                )
            ) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}
