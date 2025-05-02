package CacheDAO;
import Config.AppConfig;
import Utils.RedisCacheService;
import org.json.JSONArray;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReportCacheDAO {
    private static final String MOST_TRADED_KEY_PREFIX = "report:most_traded:";

    private static final DateTimeFormatter KEY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private String getMostTradedKey(LocalDateTime start, LocalDateTime end, int limit) {
        String startStr = (start != null) ? start.format(KEY_DATE_FORMATTER) : "NULL";
        String endStr = (end != null) ? end.format(KEY_DATE_FORMATTER) : "NULL";
        return MOST_TRADED_KEY_PREFIX + startStr + ":" + endStr + ":" + limit;
    }

    public Optional<List<Document>> findMostTraded(LocalDateTime start, LocalDateTime end, int limit) {
        String key = getMostTradedKey(start, end, limit);
        String cachedJson = RedisCacheService.getCache(key);
        if (cachedJson != null) {
            try {
                JSONArray array = new JSONArray(cachedJson);
                List<Document> documents = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    documents.add(Document.parse(array.getString(i)));
                }
                System.out.println("Cache HIT for most traded report: " + key);
                return Optional.of(documents);
            } catch (Exception e) {
                System.err.println("Error deserializing most traded report from cache (key " + key + "): " + e.getMessage());
                RedisCacheService.deleteCache(key);
            }
        }
        System.out.println("Cache MISS for most traded report: " + key);
        return Optional.empty();
    }

    public void saveMostTraded(LocalDateTime start, LocalDateTime end, int limit, List<Document> reportData) {
        String key = getMostTradedKey(start, end, limit);
        try {
            JSONArray array = new JSONArray();
            for (Document doc : reportData) {
                array.put(doc.toJson());
            }
            RedisCacheService.setCache(key, array.toString(), AppConfig.CACHE_TTL);
            System.out.println("Cache SAVED for most traded report: " + key);
        } catch (Exception e) {
            System.err.println("Error serializing most traded report for cache (key " + key + "): " + e.getMessage());
        }
    }
}
