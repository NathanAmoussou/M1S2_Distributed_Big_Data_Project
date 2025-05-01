package cacheDAO;

import java.util.List;
import java.util.Optional;

//sert un peu à rien en fait mais ça fait un peu plus propre
public interface GenericCacheDAO<T>{
    Optional<T> findByTicker(String ticker);;
    Optional<List<T>> findAll();
    Optional<List<String>> findAllTickers();
    void save(T t, int ttlSeconds);
    void saveAll(List<T> stocks, int ttlSeconds);
    void saveAllTickers(List<String> tickers, int ttlSeconds);
    void delete(String ticker);
    void invalidateAll();
    void invalidateAllTickers();

}
