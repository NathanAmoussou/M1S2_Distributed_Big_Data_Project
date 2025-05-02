package Utils;

import redis.clients.jedis.*;

public class RedisClient {
    private static RedisClient instance;
    private UnifiedJedis jedis;

    private RedisClient() {
        JedisClientConfig config = DefaultJedisClientConfig.builder()
                .user("default")
                .password("DzCxBUckZYdg5rNlysWQYPklpQC4iQtf")
                .build();

        try {
            this.jedis = new UnifiedJedis(
                    new HostAndPort("redis-14039.c80.us-east-1-2.ec2.redns.redis-cloud.com", 14039),
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
