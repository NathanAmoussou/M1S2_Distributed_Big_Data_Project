package service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.StockDAO;
import dao.StockPriceHistoryDAO;

import model.Stock;
import model.StockPriceHistory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.*;


public class StockMarketService {
    private final StockDAO stockDao;
    private final StockPriceHistoryDAO stockPriceHistoryDao;
    private final OkHttpClient httpClient = new OkHttpClient();;

    public StockMarketService(StockDAO stockDao, StockPriceHistoryDAO stockPriceHistoryDao) {
        this.stockDao = stockDao;
        this.stockPriceHistoryDao = stockPriceHistoryDao;
    }

    /**
     * Méthode pour lancer la planification (toutes les minutes
     */
    public void startScheduledUpdates() {
        System.out.println("Scheduler starting...");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("Récupération des indices boursiers...");
                // Fetch and store data for various indices...
                fetchAndStoreIndexData("DIA", "Dow Jones Industrial Average", "Industrials", "NYSE");
                fetchAndStoreIndexData("AAPL", "Apple Inc.", "Technology", "NASDAQ");
                // ... other stocks
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Erreur lors de la récupération des indices boursiers : " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);

        System.out.println("Tâche planifiée : récupération des indices toutes les minutes.");
    }


    /**
     * Méthode pour appeler l'API externe et stocker/mettre à jour les données
     * @param symbol   ex: "DIA"
     * @param stockName ex: "Dow Jones Industrial Average"
     */
    private void fetchAndStoreIndexData(String symbol, String stockName, String sector, String market) {
        System.out.println("Fetching data for symbol: " + symbol);

        String apiKey = "78X8WRL4C70SGVD7"; // à mettre dans un .env je pense
        String apiUrl = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol="
                + symbol + "&apikey=" + apiKey;

        Request request = new Request.Builder()
                .url(apiUrl)
                .build();

        BigDecimal openPrice;
        BigDecimal highPrice;
        BigDecimal lowPrice;
        BigDecimal currentPrice;

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Réponse non valide de l'API (code HTTP): " + response.code());
                return;
            }

            if (response.body() == null) {
                System.err.println("Réponse sans contenu (body nul).");
                return;
            }

            String responseBody = response.body().string();

            System.out.println(responseBody);

            JsonObject jsonObj = JsonParser.parseString(responseBody).getAsJsonObject();

            if (!jsonObj.has("Global Quote")) {
                System.err.println("Format inattendu : pas de 'Global Quote'.");
                return;
            }

            JsonObject globalQuote = jsonObj.getAsJsonObject("Global Quote");

            openPrice = globalQuote.get("02. open").getAsBigDecimal();
            highPrice = globalQuote.get("03. high").getAsBigDecimal();
            lowPrice = globalQuote.get("04. low").getAsBigDecimal();
            currentPrice = globalQuote.get("05. price").getAsBigDecimal();

        } catch (IOException e) {
            System.err.println("Erreur lors de l'appel à l'API : " + e.getMessage());
            return;
        }

//        Stock existingStock = stockDao.findById(symbol);
        Stock existingStock = stockDao.findByStockTicker(symbol);

        if (existingStock == null) {
            // Si le stock n'existe pas encore, on le crée
            Stock newStock = new Stock();
//            newStock.setStockId(symbol);
            newStock.setStockName(stockName);
            newStock.setStockTicker(symbol);
            newStock.setLastPrice(currentPrice);
            newStock.setMarket(sector);
            newStock.setSector(market);
            newStock.setLastUpdated(LocalDateTime.now());
            stockDao.save(newStock);
            System.out.println("Nouveau stock ajouté : " + newStock);
        } else {
            existingStock.setLastUpdated(LocalDateTime.now());
            stockDao.update(existingStock);
            System.out.println("Stock existant mis à jour : " + existingStock);
        }

        StockPriceHistory history = new StockPriceHistory();
//        history.setStockPriceHistoryId(null);  // NE RIEN METTRE pour Laissez MongoDB générer un ObjectId ou gérez un UUID
        history.setClosePrice(currentPrice);
        history.setHighPrice(highPrice);
        history.setLowPrice(lowPrice);
        history.setOpenPrice(openPrice);
        history.setDateTime(LocalDateTime.now());
        history.setStockPriceHistoryTicker(symbol);

        System.out.println("Données récupérées pour l'indice " + symbol);
        System.out.println(history);

        stockPriceHistoryDao.save(history);

        // 6) (Optionnel) Stocker la dernière valeur dans Redis (si besoin)
        // redisDao.saveStockPrice(symbol, currentPrice);

        System.out.println("Données mises à jour pour l'indice " + symbol + " à " + LocalDateTime.now());
    }
}