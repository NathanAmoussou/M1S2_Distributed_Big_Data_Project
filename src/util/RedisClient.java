package src.util;

public class RedisClient {
    private static RedisClient instance;
    private Jedis jedis;

    private RedisClient() {
        jedis = new Jedis("localhost", 6379);
    }

    public static synchronized RedisClient getInstance() {
        if (instance == null) {
            instance = new RedisClient();
        }
        return instance;
    }

    public Jedis getJedis() {
        return jedis;
    }
}
