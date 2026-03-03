package net.unit8.bouncr.component.store;

import tools.jackson.databind.json.JsonMapper;
import enkan.middleware.session.KeyValueStore;
import redis.clients.jedis.JedisPooled;

import java.io.Serializable;
import java.util.HashMap;

public class RedisStore implements KeyValueStore {
    private final JedisPooled jedis;
    private final JsonMapper mapper;
    private final String keyPrefix;
    private final long ttlSeconds;

    public RedisStore(JedisPooled jedis, String keyPrefix, long ttlSeconds) {
        this.jedis = jedis;
        this.mapper = JsonMapper.builder().build();
        this.keyPrefix = keyPrefix;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public Serializable read(String key) {
        String json = jedis.get(keyPrefix + key);
        if (json == null) return null;
        return mapper.readValue(json, HashMap.class);
    }

    @Override
    public String write(String key, Serializable value) {
        String json = mapper.writeValueAsString(value);
        jedis.setex(keyPrefix + key, ttlSeconds, json);
        return key;
    }

    @Override
    public String delete(String key) {
        jedis.del(keyPrefix + key);
        return key;
    }
}
