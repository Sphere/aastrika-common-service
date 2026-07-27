package org.aastrika.common;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.aastrika.common.Constants;

import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Component
@Slf4j
public class RedisCacheMgr {

    private static final int CACHE_TTL = 84600;

    private final JedisPool jedisPool =
            new JedisPool(new JedisPoolConfig(), "localhost", 6379);

    public String getCache(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(Constants.REDIS_COMMON_KEY + key);
        } catch (Exception e) {
            log.error("Error reading Redis cache", e);
            return null;
        }
    }

    public void putCache(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(Constants.REDIS_COMMON_KEY + key, CACHE_TTL, value);
        }
    }
}
