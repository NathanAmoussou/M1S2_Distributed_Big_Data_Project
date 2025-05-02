package Services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class StockPythonYahooApiService {

    // Read URL from environment variable and provide default
    private static final String DEFAULT_PYTHON_API_URL = "http://localhost:5000"; // Default for non-docker dev
    private static final String PYTHON_API_URL_ENV = System.getenv("PYTHON_API_URL");
    private static final String BASE_URL = (PYTHON_API_URL_ENV != null && !PYTHON_API_URL_ENV.isEmpty())
            ? PYTHON_API_URL_ENV
            : DEFAULT_PYTHON_API_URL;

    private static final HttpClient client = HttpClient.newHttpClient();

    static {
        System.out.println("Python API Service URL configured to: " + BASE_URL);
    }

    // Method to get stock info
    public static JSONObject getStockInfo(String stockTicker, String market) throws Exception {
        // Build the URL
        String url = BASE_URL + "/get_stock_info?stock_ticker=" + stockTicker;
        if (market != null && !market.isEmpty()) {
            url += "&market=" + market;
        }

        // Send GET request to Flask API
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Handle the response
        if (response.statusCode() == 200) {
            return new JSONObject(response.body());
        } else {
            System.out.println("Error: " + response.statusCode());
            return new JSONObject();
        }
    }

    // Method to get stock data (historical data)
    public static JSONArray getStockData(String stockTicker, String market, String startDate, String endDate) throws Exception {
        // Build the URL
        String url = BASE_URL + "/get_stock_data?stock_ticker=" + stockTicker + "&start_date=" + startDate + "&end_date=" + endDate;
        if (market != null && !market.isEmpty()) {
            url += "&market=" + market;
        }

        // Send GET request to Flask API
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Handle the response
        if (response.statusCode() == 200) {
            return new JSONArray(response.body());
        } else {
            System.out.println("Error: " + response.statusCode());
            return new JSONArray(); // Return empty array if there's an error
        }
    }

    public static void testApi() {
        try {
            // Example to get stock info for AAPL (no market specified)
            JSONObject aaplStockInfo = getStockInfo("AAPL", "");
            System.out.println("Stock Info for AAPL: " + aaplStockInfo.toString(2));

            // Example to get stock data for AAPL (no market specified) from 2023-01-01 to 2023-12-31
            JSONArray aaplStockData = getStockData("AAPL", "", "2023-01-01", "2023-12-31");
            System.out.println("Stock Data for AAPL: ");
            for (int i = 0; i < aaplStockData.length(); i++) {
                System.out.println(aaplStockData.getJSONObject(i).toString(2));
            }

            // Example to get stock info for LVMH (MC) on Euronext Paris (PA)
            JSONObject lvmhStockInfo = getStockInfo("MC", "PA");
            System.out.println("Stock Info for LVMH: " + lvmhStockInfo.toString(2));

            // Example to get stock data for LVMH (MC) on Euronext Paris (PA) from 2023-01-01 to 2023-12-31
            JSONArray lvmhStockData = getStockData("MC", "PA", "2023-01-01", "2023-12-31");
            System.out.println("Stock Data for LVMH: ");
            for (int i = 0; i < lvmhStockData.length(); i++) {
                System.out.println(lvmhStockData.getJSONObject(i).toString(2));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
