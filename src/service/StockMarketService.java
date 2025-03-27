package src.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import src.dao.StockDAO;
import src.dao.StockPriceHistoryDAO;
import src.model.Stock;
import src.model.StockPriceHistory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.io.IOException;
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
     * Méthode pour lancer la planification (toutes les minutes).
     * Note : ne pas fermer le scheduler immédiatement pour qu'il tourne en continu.
     */
    public void startScheduledUpdates() {
        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    //TODO: Ajouter d'autres indices si nécessaire et automatiser
                    fetchAndStoreIndexData("DIA", "Dow Jones Industrial Average", "Industrials", "NYSE");
                    fetchAndStoreIndexData("AAPL", "Apple Inc.", "Technology", "NASDAQ");
                    fetchAndStoreIndexData("GOOGL", "Alphabet Inc.", "Communication Services", "NASDAQ");
                    fetchAndStoreIndexData("MSFT", "Microsoft Corporation", "Technology", "NASDAQ");
                    fetchAndStoreIndexData("AMZN", "Amazon.com Inc.", "Consumer Cyclical", "NASDAQ");
                    fetchAndStoreIndexData("TSLA", "Tesla Inc.", "Consumer Cyclical", "NASDAQ");
                    fetchAndStoreIndexData("FB", "Meta Platforms Inc.", "Communication Services", "NASDAQ");
                    fetchAndStoreIndexData("NVDA", "NVIDIA Corporation", "Technology", "NASDAQ");
                    fetchAndStoreIndexData("BTC", "Bitcoin", "Cryptocurrency", "Crypto");
                    fetchAndStoreIndexData("ETH", "Ethereum", "Cryptocurrency", "Crypto");

                    fetchAndStoreIndexData("EURUSD", "Euro/US Dollar", "Forex", "Forex");
                    fetchAndStoreIndexData("GBPUSD", "British Pound/US Dollar", "Forex", "Forex");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 1, TimeUnit.MINUTES);
        }

        System.out.println("Tâche planifiée : récupération des indices toutes les minutes.");
    }

    /**
     * Méthode pour appeler l'API externe et stocker/mettre à jour les données
     * @param symbol   ex: "DIA"
     * @param stockName ex: "Dow Jones Industrial Average"
     */
    private void fetchAndStoreIndexData(String symbol, String stockName, String sector, String market) {
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

        Stock existingStock = stockDao.findById(symbol);

        if (existingStock == null) {
            // Si le stock n'existe pas encore, on le crée
            Stock newStock = new Stock();
            newStock.setStockId(symbol);
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
        history.setStockPriceHistoryId(null);  // Laissez MongoDB générer un ObjectId ou gérez un UUID
        history.setClosePrice(currentPrice);
        history.setHighPrice(highPrice);
        history.setLowPrice(lowPrice);
        history.setOpenPrice(openPrice);
        history.setDateTime(LocalDateTime.now());
        history.setStockPriceHistoryId(symbol);


        System.out.println("Données récupérées pour l'indice " + symbol);
        System.out.println(history);

        stockPriceHistoryDao.save(history);

        // 6) (Optionnel) Stocker la dernière valeur dans Redis (si besoin)
        // redisDao.saveStockPrice(symbol, currentPrice);

        System.out.println("Données mises à jour pour l'indice " + symbol + " à " + LocalDateTime.now());
    }
}