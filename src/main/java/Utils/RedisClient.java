package Utils;

import redis.clients.jedis.*;

public class RedisClient {
    private static RedisClient instance;
    private UnifiedJedis jedis;

    private RedisClient() {
        JedisClientConfig config = DefaultJedisClientConfig.builder()
                .user("default")
                .password("1CXFwOcDEeS5osBZ8jrFD8ugxQGlF9wM")
                .build();

        try {
            this.jedis = new UnifiedJedis(
                    new HostAndPort("redis-13268.c339.eu-west-3-1.ec2.redns.redis-cloud.com", 13268),
                    config
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static synchronized RedisClient getInstance() {
        if (instance == null) {
            instance = new RedisClient();
        }
        return instance;
    }

    public UnifiedJedis getJedis() {
        return jedis;
    }
}
